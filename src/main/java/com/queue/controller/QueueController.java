package com.queue.controller;

import com.queue.model.Customer;
import com.queue.service.CounterService;
import com.queue.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @Autowired
    private CounterService counterService;

    @PostMapping("/queue/ticket")
    public ResponseEntity<Map<String, Object>> addTicket(
            @RequestParam String priority,
            @RequestParam(required = false, defaultValue = "Bank Service") String serviceType) {
        
        Customer customer = queueService.addCustomer(serviceType, priority);

        Map<String, Object> response = new HashMap<>();
        response.put("ticketNumber", customer.getTicketNumber());
        response.put("priority", customer.getPriority().name());
        response.put("serviceType", customer.getServiceType());
        response.put("status", customer.getStatus());
        response.put("estimatedWaitMinutes", customer.getEstimatedWaitTimeMinutes());
        response.put("queuePosition", queueService.getQueueSize());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queue/waiting")
    public ResponseEntity<List<Customer>> getWaitingQueue() {
        return ResponseEntity.ok(queueService.getQueueSnapshot());
    }

    @GetMapping("/queue/served")
    public ResponseEntity<List<Customer>> getServedHistory() {
        return ResponseEntity.ok(queueService.getServedHistory());
    }

    @GetMapping("/queue/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssued", queueService.getTotalTicketsIssued());
        stats.put("totalServed", queueService.getTotalTicketsServed());
        stats.put("totalCancelled", queueService.getTotalTicketsCancelled());
        stats.put("totalNoShow", queueService.getTotalTicketsNoShow());
        stats.put("averageWaitTime", queueService.getAverageWaitTime()); // mins
        stats.put("averageActualWaitTime", queueService.getAverageActualWaitTimeSeconds()); // secs
        stats.put("averageServiceDuration", queueService.getAverageServiceDurationSeconds()); // secs
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/queue/serve-next")
    public ResponseEntity<Map<String, String>> serveNext() {
        boolean served = counterService.serveNextAny();
        if (served) {
            return ResponseEntity.ok(Map.of("message", "Called next ticket."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "No tickets waiting or all counters busy."));
        }
    }

    @PostMapping("/queue/complete/{counterId}/{ticketNumber}")
    public ResponseEntity<Map<String, String>> completeTicket(@PathVariable int counterId, @PathVariable String ticketNumber) {
        boolean completed = counterService.completeTicket(counterId, ticketNumber);
        if (completed) {
            return ResponseEntity.ok(Map.of("message", "Ticket " + ticketNumber + " completed."));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Failed to complete ticket."));
    }

    @PostMapping("/queue/no-show/{counterId}/{ticketNumber}")
    public ResponseEntity<Map<String, String>> markNoShow(@PathVariable int counterId, @PathVariable String ticketNumber) {
        boolean marked = counterService.markNoShow(counterId, ticketNumber);
        if (marked) {
            return ResponseEntity.ok(Map.of("message", "Ticket " + ticketNumber + " marked as No-Show."));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Failed to mark no-show."));
    }

    @PostMapping("/queue/cancel/{ticketNumber}")
    public ResponseEntity<Map<String, String>> cancelTicket(@PathVariable String ticketNumber) {
        boolean cancelled = queueService.cancelTicket(ticketNumber);
        if (cancelled) {
            return ResponseEntity.ok(Map.of("message", "Ticket " + ticketNumber + " has been cancelled."));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Ticket not found in waiting queue."));
    }

    @PutMapping("/queue/update/{ticketNumber}")
    public ResponseEntity<Map<String, String>> updateTicket(
            @PathVariable String ticketNumber,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String priority) {
        boolean updated = queueService.updateTicket(ticketNumber, serviceType, priority);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Ticket " + ticketNumber + " updated."));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Ticket not found in waiting queue."));
    }

    @PostMapping("/queue/reset")
    public ResponseEntity<Map<String, String>> resetQueue() {
        queueService.resetQueue();
        counterService.pauseService();
        return ResponseEntity.ok(Map.of("message", "System reset successfully."));
    }

    @GetMapping("/queue/export")
    public ResponseEntity<String> exportReport() {
        List<Customer> history = queueService.getServedHistory();
        StringBuilder csv = new StringBuilder();
        csv.append("Ticket Number,Priority,Service Type,Status,Counter,Created Time,Service Started Time,Completed Time,Actual Wait Time (s),Service Duration (s)\n");
        for (Customer c : history) {
            csv.append(String.format("%s,%s,%s,%s,%d,%s,%s,%s,%d,%d\n",
                    c.getTicketNumber(),
                    c.getPriority().name(),
                    c.getServiceType(),
                    c.getStatus(),
                    c.getCounterNumber(),
                    c.getCreatedTime() != null ? c.getCreatedTime().toString() : "",
                    c.getServiceStartTime() != null ? c.getServiceStartTime().toString() : "",
                    c.getServiceCompletedTime() != null ? c.getServiceCompletedTime().toString() : "",
                    c.getActualWaitTimeSeconds(),
                    c.getServiceDurationSeconds()
            ));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=queue_report.csv");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok().headers(headers).body(csv.toString());
    }

    @GetMapping("/queue/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> state = new HashMap<>();

        // Demo State
        state.put("isDemoRunning", counterService.isServiceRunning());

        // Counters
        Map<Integer, Customer> counters = queueService.getCountersState();
        Map<String, Object> countersMap = new HashMap<>();
        counters.forEach((id, customer) -> {
            Map<String, Object> counterInfo = new HashMap<>();
            if (customer != null) {
                counterInfo.put("serving", customer.getTicketNumber());
                counterInfo.put("priority", customer.getPriority().name());
                counterInfo.put("status", customer.getStatus());
                counterInfo.put("serviceType", customer.getServiceType());
                counterInfo.put("serviceStartTime", customer.getServiceStartTime() != null ? customer.getServiceStartTime().toString() : null);
            } else {
                counterInfo.put("serving", null);
                counterInfo.put("status", "IDLE");
            }
            countersMap.put(String.valueOf(id), counterInfo);
        });
        state.put("counters", countersMap);

        // Queue
        List<Customer> queue = queueService.getQueueSnapshot();
        state.put("queueSize", queue.size());
        state.put("queue", queue.stream().map(c -> {
            Map<String, Object> item = new HashMap<>();
            item.put("ticketNumber", c.getTicketNumber());
            item.put("priority", c.getPriority().name());
            item.put("status", c.getStatus());
            item.put("serviceType", c.getServiceType());
            return item;
        }).toList());

        // History
        List<Customer> history = queueService.getServedHistory();
        state.put("history", history);
        
        // Stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssued", queueService.getTotalTicketsIssued());
        stats.put("totalServed", queueService.getTotalTicketsServed());
        stats.put("totalCancelled", queueService.getTotalTicketsCancelled());
        stats.put("totalNoShow", queueService.getTotalTicketsNoShow());
        stats.put("averageWaitTime", queueService.getAverageWaitTime()); // mins
        stats.put("averageActualWaitTime", queueService.getAverageActualWaitTimeSeconds()); // secs
        stats.put("averageServiceDuration", queueService.getAverageServiceDurationSeconds()); // secs
        state.put("stats", stats);

        return ResponseEntity.ok(state);
    }
    
    // --- Demo Control Endpoints --- //

    @PostMapping("/demo/start")
    public ResponseEntity<Map<String, String>> startDemo() {
        counterService.startService();
        return ResponseEntity.ok(Map.of("message", "Service running automatically."));
    }

    @PostMapping("/demo/pause")
    public ResponseEntity<Map<String, String>> pauseDemo() {
        counterService.pauseService();
        return ResponseEntity.ok(Map.of("message", "Service paused."));
    }

    @PostMapping("/demo/next")
    public ResponseEntity<Map<String, String>> serveNextDemo() {
        return serveNext();
    }

    @PostMapping("/demo/reset")
    public ResponseEntity<Map<String, String>> resetDemo() {
        return resetQueue();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPriority(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
