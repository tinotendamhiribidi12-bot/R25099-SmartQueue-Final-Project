# SmartQueue Management System

SmartQueue is a Java Spring Boot final project that manages customer queues using priority levels, service counters, and a web-based interface.

## Project Features

- Add customers to a smart queue
- Assign priority based on customer/emergency category
- Serve customers through counters
- Display queue and service status in the browser
- Store queue records using SQLite
- Includes frontend files built with HTML, CSS, and JavaScript

## Technologies Used

- Java 17
- Spring Boot 3.2.4
- Maven
- SQLite JDBC
- HTML, CSS, JavaScript

## Main Files to Review

- `src/main/java/com/queue/SmartQueueApplication.java` - starts the Spring Boot application
- `src/main/java/com/queue/controller/QueueController.java` - handles web/API requests
- `src/main/java/com/queue/service/QueueService.java` - manages queue logic and priority ordering
- `src/main/java/com/queue/service/CounterService.java` - simulates service counters
- `src/main/java/com/queue/db/DatabaseHandler.java` - connects the system to SQLite
- `src/main/java/com/queue/model/Customer.java` - represents a customer/ticket in the queue
- `src/main/resources/static/index.html` - main user interface
- `src/main/resources/static/display.html` - display screen
- `src/main/resources/static/js/app.js` - frontend behavior
- `src/main/resources/static/css/style.css` - styling/design

## How to Run the Project

1. Open the project in IntelliJ IDEA.
2. Make sure Java 17 is installed and selected.
3. Wait for Maven to load the dependencies from `pom.xml`.
4. Run `SmartQueueApplication.java`.
5. Open a browser and go to:

```text
http://localhost:8080
```

## Database

The project uses an SQLite database file named `queue_data.db`.

## Student Details

Student Number: R25099
Project: SmartQueue Final Project
