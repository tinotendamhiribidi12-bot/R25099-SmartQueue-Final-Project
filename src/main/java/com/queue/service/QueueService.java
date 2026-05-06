package com.queue.service;

import com.queue.db.DatabaseHandler;
import com.queue.model.Customer;
import com.queue.model.PriorityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service demonstrating Multithreading thread-safe collections (PriorityBlockingQueue),
 * and core Object-Oriented principles.
 */
@Service
public class QueueService {

    // Thread-safe PriorityQueue that automatically orders Customers based on their compareTo logic
    private final PriorityBlockingQueue<Customer> queue = new PriorityBlockingQueue<>();
    
    // Atomic variables ensure thread-safe incrementing without explicit locks
    private final AtomicInteger regularCounter = new AtomicInteger(1);
    private final AtomicInteger elderlyCounter = new AtomicInteger(1);
    private final AtomicInteger emergencyCounter = new AtomicInteger(1);

    // Tracks which customer each counter is serving (null = idle)
    private final Map<Integer, Customer> countersState = Collections.synchronizedMap(new LinkedHashMap<>());

    // History to keep track of completed tasks for stats
    private final List<Customer> servedHistory = Collections.synchronizedList(new LinkedList<>());
    private int totalTicketsIssued = 0;

    @Autowired
    private DatabaseHandler databaseHandler;

    public QueueService() {
        // 3 active counters initialized as idle (null)
        countersState.put(1, null);
        countersState.put(2, null);
        countersState.put(3, null);
    }

    public Customer addCustomer(String serviceType, String priorityStr) {
        PriorityType priority;
        String pPrefix;
        int num;
        try {
            priority = PriorityType.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority category");
        }
        
        switch (priority) {
            case EMERGENCY: 
                pPrefix = "E"; 
                num = emergencyCounter.getAndIncrement();
                break;
            case ELDERLY:   
                pPrefix = "P"; 
                num = elderlyCounter.getAndIncrement();
                break;
            default:        
                pPrefix = "R"; 
                num = regularCounter.getAndIncrement();
                break;
        }

        String sPrefix = "U";
        if (serviceType != null) {
            if (serviceType.contains("Bank")) sPrefix = "B";
            else if (serviceType.contains("Hospital")) sPrefix = "H";
            else if (serviceType.contains("Government") || serviceType.contains("Gov")) sPrefix = "G";
        }

        String ticketNumber = sPrefix + "-" + pPrefix + "-" + String.format("%03d", num);
        
        int waitTime = getEstimatedWaitTime(priority);
        
        Customer customer = new Customer(ticketNumber, serviceType, priority, waitTime);
        queue.add(customer);
        totalTicketsIssued++;
        
        if (databaseHandler != null) {
            databaseHandler.saveTicket(customer);
            databaseHandler.logAction("TICKET_CREATED", ticketNumber, "Generated ticket for " + serviceType + " with priority " + priority.name());
        }

        return customer;
    }

    public Customer pollNextCustomer() {
        return queue.poll();
    }

    public List<Customer> getQueueSnapshot() {
        List<Customer> sorted = new ArrayList<>(queue);
        Collections.sort(sorted);
        return sorted;
    }

    public Map<Integer, Customer> getCountersState() {
        return countersState;
    }

    public void setCounterState(int counterId, Customer customer) {
        countersState.put(counterId, customer);
    }
    
    public void addCompletedCustomer(Customer customer) {
        if (customer.getServiceStartTime() != null && customer.getCreatedTime() != null) {
            long waitSecs = Duration.between(customer.getCreatedTime(), customer.getServiceStartTime()).getSeconds();
            customer.setActualWaitTimeSeconds(waitSecs);
        }
        if (customer.getServiceCompletedTime() != null && customer.getServiceStartTime() != null) {
            long durationSecs = Duration.between(customer.getServiceStartTime(), customer.getServiceCompletedTime()).getSeconds();
            customer.setServiceDurationSeconds(durationSecs);
        }
        servedHistory.add(customer);
        if (servedHistory.size() > 200) {
            servedHistory.remove(0); // Keep last 200
        }
        if (databaseHandler != null) {
            databaseHandler.updateTicket(customer);
        }
    }
    
    public List<Customer> getServedHistory() {
        return new ArrayList<>(servedHistory);
    }

    public int getQueueSize() {
        return queue.size();
    }
    
    public int getTotalTicketsIssued() {
        return totalTicketsIssued;
    }
    
    public int getTotalTicketsServed() {
        return (int) servedHistory.stream().filter(c -> "SERVED".equals(c.getStatus())).count();
    }

    public int getTotalTicketsCancelled() {
        return (int) servedHistory.stream().filter(c -> "CANCELLED".equals(c.getStatus())).count();
    }

    public int getTotalTicketsNoShow() {
        return (int) servedHistory.stream().filter(c -> "NO_SHOW".equals(c.getStatus())).count();
    }

    public int getAverageWaitTime() {
        List<Customer> served = servedHistory.stream().filter(c -> "SERVED".equals(c.getStatus())).toList();
        if (served.isEmpty()) return 0;
        long totalWaitMinutes = 0;
        for (Customer c : served) {
            totalWaitMinutes += c.getEstimatedWaitTimeMinutes(); 
        }
        return (int) (totalWaitMinutes / served.size());
    }

    public int getAverageActualWaitTimeSeconds() {
        List<Customer> served = servedHistory.stream().filter(c -> "SERVED".equals(c.getStatus())).toList();
        if (served.isEmpty()) return 0;
        long totalWaitSecs = 0;
        for (Customer c : served) {
            totalWaitSecs += c.getActualWaitTimeSeconds(); 
        }
        return (int) (totalWaitSecs / served.size());
    }

    public int getAverageServiceDurationSeconds() {
        List<Customer> served = servedHistory.stream().filter(c -> "SERVED".equals(c.getStatus())).toList();
        if (served.isEmpty()) return 0;
        long totalDurationSecs = 0;
        for (Customer c : served) {
            totalDurationSecs += c.getServiceDurationSeconds(); 
        }
        return (int) (totalDurationSecs / served.size());
    }

    /**
     * Estimated wait time dynamically calculated based on:
     * - Queue ahead of this priority
     * - Realistic service time estimate (e.g. 20s = ~0.33 min)
     * - Number of active counters available
     */
    public int getEstimatedWaitTime(PriorityType priority) {
        int aheadCount = 0;
        for (Customer c : queue) {
            if (c.getPriority().getLevel() <= priority.getLevel()) {
                aheadCount++;
            }
        }
        
        int activeCounters = 3;
        // Assume avg service is 1 min for demo simplicity
        int avgServiceMins = 1; 
        
        if (activeCounters == 0) return 0; 
        
        return (aheadCount * avgServiceMins) / activeCounters;
    }

    public boolean cancelTicket(String ticketNumber) {
        for (Customer c : queue) {
            if (c.getTicketNumber().equals(ticketNumber)) {
                if (queue.remove(c)) {
                    c.setStatus("CANCELLED");
                    addCompletedCustomer(c);
                    if (databaseHandler != null) {
                        databaseHandler.logAction("TICKET_CANCELLED", ticketNumber, "Ticket was cancelled");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean updateTicket(String ticketNumber, String newServiceType, String newPriorityStr) {
        for (Customer c : queue) {
            if (c.getTicketNumber().equals(ticketNumber)) {
                if (queue.remove(c)) {
                    if (newServiceType != null && !newServiceType.isEmpty()) {
                        c.setServiceType(newServiceType);
                    }
                    if (newPriorityStr != null && !newPriorityStr.isEmpty()) {
                        try {
                            c.setPriority(PriorityType.valueOf(newPriorityStr.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    // Re-add to queue to adjust priority
                    queue.add(c);
                    if (databaseHandler != null) {
                        databaseHandler.updateTicket(c);
                        databaseHandler.logAction("TICKET_UPDATED", ticketNumber, "Updated to priority: " + c.getPriority() + " / " + c.getServiceType());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resets the entire system for Demo purposes
     */
    public void resetQueue() {
        queue.clear();
        servedHistory.clear();
        countersState.put(1, null);
        countersState.put(2, null);
        countersState.put(3, null);
        regularCounter.set(1);
        elderlyCounter.set(1);
        emergencyCounter.set(1);
        totalTicketsIssued = 0;
        if (databaseHandler != null) {
            databaseHandler.logAction("DEMO_RESET", null, "System was reset");
        }
    }
}
