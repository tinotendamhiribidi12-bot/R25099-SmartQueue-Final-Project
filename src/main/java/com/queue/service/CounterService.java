package com.queue.service;

import com.queue.db.DatabaseHandler;
import com.queue.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service demonstrating Multithreading by allocating separate Threads 
 * for each Bank/Hospital counter. Allows for Demo pausing and manual progression.
 */
@Service
public class CounterService implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private QueueService queueService;

    @Autowired
    private DatabaseHandler databaseHandler;

    // True means Demo Mode is running automatically (every ~20s). False means paused.
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Create 3 daemon threads for the counters for auto demo
        for (int i = 1; i <= 3; i++) {
            final int counterId = i;
            Thread t = new Thread(() -> runCounter(counterId), "counter-" + counterId);
            t.setDaemon(true);
            t.start();
        }
    }

    private void runCounter(int counterId) {
        System.out.println("Counter " + counterId + " thread started.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isRunning.get()) {
                    Customer current = queueService.getCountersState().get(counterId);
                    if (current == null) {
                        serveNextForCounter(counterId, true);
                        Thread.sleep(2000); 
                    } else {
                        // In auto mode, wait 15 seconds then complete
                        Thread.sleep(15000);
                        if (isRunning.get() && current.getStatus().equals("SERVING")) {
                            completeTicket(counterId, current.getTicketNumber());
                        }
                    }
                } else {
                    // Paused - just idle
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static long lastTicketDispenseTime = 0;
    private static final long DISPENSE_STAGGER_MS = 2000; // 2 seconds visible stagger

    /**
     * Executes the service logic for a given counter.
     * @param counterId ID of the counter serving
     * @param isAuto true if called by auto-run, false if manual click
     */
    public synchronized boolean serveNextForCounter(int counterId, boolean isAuto) {
        Customer current = queueService.getCountersState().get(counterId);
        if (current != null) {
            return false;
        }

        // Stagger assignments slightly in auto mode so viewer sees Priority logic clearly
        if (isAuto) {
            long now = System.currentTimeMillis();
            if (now - lastTicketDispenseTime < DISPENSE_STAGGER_MS) {
                return false; // Skip this tick so we don't grab 3 tickets concurrently
            }
        }

        Customer customer = queueService.pollNextCustomer();
        if (customer != null) {
            if (isAuto) lastTicketDispenseTime = System.currentTimeMillis();
            
            customer.setStatus("SERVING");
            customer.setServiceStartTime(java.time.LocalDateTime.now());
            customer.setCounterNumber(counterId);
            queueService.setCounterState(counterId, customer);
            System.out.println("Counter " + counterId + " serving: " + customer.getTicketNumber());

            if (databaseHandler != null) {
                databaseHandler.updateTicket(customer);
                databaseHandler.logAction("TICKET_CALLED", customer.getTicketNumber(), "Ticket called to Counter " + counterId);
            }
            return true;
        }
        return false;
    }

    public synchronized boolean completeTicket(int counterId, String ticketNumber) {
        Customer current = queueService.getCountersState().get(counterId);
        if (current != null && current.getTicketNumber().equals(ticketNumber)) {
            current.setStatus("SERVED");
            current.setServiceCompletedTime(java.time.LocalDateTime.now());
            queueService.addCompletedCustomer(current);
            queueService.setCounterState(counterId, null);
            System.out.println("Counter " + counterId + " completed: " + current.getTicketNumber());

            if (databaseHandler != null) {
                databaseHandler.logAction("TICKET_COMPLETED", ticketNumber, "Ticket completed at Counter " + counterId);
            }
            return true;
        }
        return false;
    }

    public synchronized boolean markNoShow(int counterId, String ticketNumber) {
        Customer current = queueService.getCountersState().get(counterId);
        if (current != null && current.getTicketNumber().equals(ticketNumber)) {
            current.setStatus("NO_SHOW");
            current.setServiceCompletedTime(java.time.LocalDateTime.now());
            queueService.addCompletedCustomer(current);
            queueService.setCounterState(counterId, null);
            System.out.println("Counter " + counterId + " marked no-show: " + current.getTicketNumber());

            if (databaseHandler != null) {
                databaseHandler.logAction("TICKET_NOSHOW", ticketNumber, "Ticket marked as No-Show at Counter " + counterId);
            }
            return true;
        }
        return false;
    }

    // --- Demo Mode Control Endpoints ---

    public void startService() {
        isRunning.set(true);
    }

    public void pauseService() {
        isRunning.set(false);
    }

    public boolean isServiceRunning() {
        return isRunning.get();
    }

    public boolean serveNextAny() {
        // Find an idle counter to assign
        for (int i = 1; i <= 3; i++) {
            Customer c = queueService.getCountersState().get(i);
            if (c == null) {
                return serveNextForCounter(i, false);
            }
        }
        return false;
    }
}
