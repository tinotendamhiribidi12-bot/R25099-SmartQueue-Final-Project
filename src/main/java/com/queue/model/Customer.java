package com.queue.model;

import java.time.LocalDateTime;

/**
 * Demonstrates Object-Oriented Programming (Encapsulation and Abstraction) 
 * by hiding data fields and providing getter/setter access. 
 * Implements Comparable to allow the PriorityBlockingQueue to order customers dynamically.
 */
public class Customer implements Comparable<Customer> {
    private String ticketNumber;
    private String serviceType; // e.g., Bank Service, Hospital Service, Government Office
    private PriorityType priority; // EMERGENCY(1), ELDERLY(2), REGULAR(3)
    private LocalDateTime createdTime;
    private LocalDateTime serviceStartTime;
    private LocalDateTime serviceCompletedTime;
    private int counterNumber;
    private int estimatedWaitTimeMinutes;
    private long actualWaitTimeSeconds; // actual wait time
    private long serviceDurationSeconds;
    private String status; // WAITING, SERVING, SERVED, CANCELLED, NO_SHOW

    public Customer() {}

    public Customer(String ticketNumber, String serviceType, PriorityType priority, int estimatedWaitTimeMinutes) {
        this.ticketNumber = ticketNumber;
        this.serviceType = serviceType;
        this.priority = priority;
        this.createdTime = LocalDateTime.now();
        this.status = "WAITING";
        this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
    }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public PriorityType getPriority() { return priority; }
    public void setPriority(PriorityType priority) { this.priority = priority; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(LocalDateTime serviceStartTime) { this.serviceStartTime = serviceStartTime; }

    public LocalDateTime getServiceCompletedTime() { return serviceCompletedTime; }
    public void setServiceCompletedTime(LocalDateTime serviceCompletedTime) { this.serviceCompletedTime = serviceCompletedTime; }

    public int getCounterNumber() { return counterNumber; }
    public void setCounterNumber(int counterNumber) { this.counterNumber = counterNumber; }

    public int getEstimatedWaitTimeMinutes() { return estimatedWaitTimeMinutes; }
    public void setEstimatedWaitTimeMinutes(int estimatedWaitTimeMinutes) { this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes; }

    public long getActualWaitTimeSeconds() { return actualWaitTimeSeconds; }
    public void setActualWaitTimeSeconds(long actualWaitTimeSeconds) { this.actualWaitTimeSeconds = actualWaitTimeSeconds; }

    public long getServiceDurationSeconds() { return serviceDurationSeconds; }
    public void setServiceDurationSeconds(long serviceDurationSeconds) { this.serviceDurationSeconds = serviceDurationSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Determines PriorityQueue logic: Emergency first, then Elderly/Disabled, then Regular.
     * Within the same category, uses First-Come-First-Served (FCFS) based on arrival time.
     */
    @Override
    public int compareTo(Customer o) {
        // Lower level means higher priority (e.g., EMERGENCY=1 is higher than REGULAR=3)
        if (this.priority.getLevel() != o.priority.getLevel()) {
            return Integer.compare(this.priority.getLevel(), o.priority.getLevel());
        }
        // If same priority, First-In-First-Out based on arrival time
        return this.createdTime.compareTo(o.createdTime);
    }
}
