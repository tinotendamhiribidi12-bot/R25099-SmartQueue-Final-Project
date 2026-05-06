package com.queue.db;

import com.queue.model.Customer;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

/**
 * Demonstrates JDBC implementation for persistent storage to SQLite.
 */
@Component
public class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:queue_data.db";

    @PostConstruct
    public void initialize() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // Drop old tables to allow new schema
            stmt.execute("DROP TABLE IF EXISTS customer_history");
            stmt.execute("DROP TABLE IF EXISTS tickets");
            stmt.execute("DROP TABLE IF EXISTS audit_logs");
            
            String createTickets = "CREATE TABLE IF NOT EXISTS tickets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ticket_number TEXT NOT NULL, " +
                    "service_type TEXT, " +
                    "priority TEXT, " +
                    "status TEXT, " +
                    "counter_number INTEGER, " +
                    "created_at TEXT, " +
                    "service_started_at TEXT, " +
                    "service_completed_at TEXT, " +
                    "actual_wait_seconds INTEGER, " +
                    "service_duration_seconds INTEGER" +
                    ")";
            stmt.execute(createTickets);

            String createAuditLogs = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "action TEXT NOT NULL, " +
                    "ticket_number TEXT, " +
                    "details TEXT, " +
                    "created_at TEXT" +
                    ")";
            stmt.execute(createAuditLogs);
            System.out.println("Database initialized successfully with new schema.");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    public void saveTicket(Customer customer) {
        String insertSQL = "INSERT INTO tickets(ticket_number, service_type, priority, status, counter_number, created_at, service_started_at, service_completed_at, actual_wait_seconds, service_duration_seconds) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setString(1, customer.getTicketNumber());
            pstmt.setString(2, customer.getServiceType());
            pstmt.setString(3, customer.getPriority().name());
            pstmt.setString(4, customer.getStatus());
            pstmt.setInt(5, customer.getCounterNumber());
            pstmt.setString(6, customer.getCreatedTime() != null ? customer.getCreatedTime().toString() : null);
            pstmt.setString(7, customer.getServiceStartTime() != null ? customer.getServiceStartTime().toString() : null);
            pstmt.setString(8, customer.getServiceCompletedTime() != null ? customer.getServiceCompletedTime().toString() : null);
            pstmt.setLong(9, customer.getActualWaitTimeSeconds());
            pstmt.setLong(10, customer.getServiceDurationSeconds());
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to save ticket data: " + e.getMessage());
        }
    }

    public void updateTicket(Customer customer) {
        String updateSQL = "UPDATE tickets SET status=?, counter_number=?, service_started_at=?, service_completed_at=?, actual_wait_seconds=?, service_duration_seconds=? WHERE ticket_number=?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            pstmt.setString(1, customer.getStatus());
            pstmt.setInt(2, customer.getCounterNumber());
            pstmt.setString(3, customer.getServiceStartTime() != null ? customer.getServiceStartTime().toString() : null);
            pstmt.setString(4, customer.getServiceCompletedTime() != null ? customer.getServiceCompletedTime().toString() : null);
            pstmt.setLong(5, customer.getActualWaitTimeSeconds());
            pstmt.setLong(6, customer.getServiceDurationSeconds());
            pstmt.setString(7, customer.getTicketNumber());
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update ticket data: " + e.getMessage());
        }
    }

    public void logAction(String action, String ticketNumber, String details) {
        String insertSQL = "INSERT INTO audit_logs(action, ticket_number, details, created_at) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setString(1, action);
            pstmt.setString(2, ticketNumber);
            pstmt.setString(3, details);
            pstmt.setString(4, LocalDateTime.now().toString());
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }
}
