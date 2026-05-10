# SmartQueue Management System

**Tinotenda Mhiribidi | Java 2026 Final Project**

## Overview
SmartQueue is a professional-grade, intelligent queue management web application designed to streamline customer service flows across various industries (Banking, Healthcare, Government). Built using Java Spring Boot and a robust backend architecture, the system accurately handles priority-based queues, calculates dynamic wait times, and persists real-time data using an SQLite database. 

It features a "girly but professional" aesthetic with purple, navy, and lavender themes, and comes with a functional Demo Mode to easily showcase the project's capabilities.

## Key Features
*   **Priority Queueing Logic:** Utilizes custom Java `PriorityQueue` implementations to accurately sort customers based on urgency (Emergency, Elderly/Disabled, Regular).
*   **Service Counters:** Three distinct service counters with manual ticket completion, "no-show" controls, and multithreaded background processing.
*   **Demo Mode:** An automated mode that automatically processes tickets to demonstrate the application's flow and multithreading capabilities.
*   **Analytics Dashboard:** Real-time visual data reporting utilizing Chart.js, featuring statistics on issued/served tickets, average wait times, and ticket statuses.
*   **Customer Display Board:** A dedicated view for waiting customers to track queue positions and see when they are called to a counter.
*   **Persistent Data Storage:** Uses JDBC/SQLite to store historical ticket information, allowing for comprehensive reporting and CSV exports.

## Technologies Used
*   **Backend:** Java 17+, Spring Boot, JDBC
*   **Database:** SQLite
*   **Frontend:** HTML5, CSS3 (Vanilla), JavaScript, Chart.js

## How to Run the Application
1. Ensure you have Java 17+ and Maven installed.
2. Open the project in your preferred IDE (IntelliJ IDEA, Eclipse, etc.) or open a terminal in the project root.
3. Run the Spring Boot application using Maven:
   ```bash
   mvn spring-boot:run
   ```
4. Open your web browser and navigate to:
   * **Main Dashboard:** `http://localhost:8080/`
   * **Customer Display Board:** `http://localhost:8080/display.html`

## Usage Instructions
*   **Generate Ticket:** Select a Service Type (Bank, Hospital, etc.) and a Category (Regular, Elderly, Emergency) to issue a new ticket.
*   **Service Counters:** Use the "Serve Next Ticket" button to manually pull the highest priority waiting customer to an available counter. Alternatively, use "Auto Demo Mode" to simulate real traffic.
*   **History & Exports:** View all generated tickets in the History table at the bottom of the page. You can filter by priority/status and export the data to a CSV file.

## Developer
*   **Name:** Tinotenda Mhiribidi
*   **Course:** Programming in Java
*   **Year:** 2026
