# Email Queue Service

A high-performance microservice designed for asynchronous email processing. This system ensures reliable email delivery by queuing requests and using a scheduled retry mechanism for failed attempts.

## Core Capabilities

*   **Asynchronous Processing**: Immediate API response while emails are sent in the background.
*   **Reliability**: Automatic retry logic for failed deliveries (configurable max retries).
*   **Data Integrity**: Hash-based duplicate detection prevents sending the same email twice.
*   **Validation**: Robust input validation for all email requests.
*   **Observability**: Health checks and detailed logging.

## Tech Stack

*   **Framework**: Spring Boot 3
*   **Language**: Java 21
*   **Database**: MySQL 8.0 (Dockerized)
*   **Build Tool**: Maven

## Installation & Setup

### 1. Database Setup
Start the MySQL container using Docker Compose:

```bash
docker-compose up -d
```

### 2. Configuration
The application uses environment variables for security. You can set these in a `.env` file (see `.env.example`) or directly in your environment.

**Required Variables:**
*   `DB_PASSWORD`
*   `MAIL_USERNAME` (for Mailtrap/SMTP)
*   `MAIL_PASSWORD` (for Mailtrap/SMTP)

### 3. Running the Service
Use the Maven wrapper to start the application:

```bash
./mvnw spring-boot:run
```

The server will initialize on port **8085**.

## API Reference

### Send an Email
Add a new email to the processing queue.

**POST** `/api/v1/mails`

**Example Payload:**

```json
{
  "senderEmail": "notifications@startup.io",
  "recipientEmail": "client.service@business.com",
  "subject": "Invoice #8392 - Payment Confirmation",
  "body": "Your payment has been successfully processed. Thank you for your business."
}
```

**Responses:**

*   `201 Created`: Email successfully queued.
*   `200 OK`: Duplicate email detected (skipped).
*   `400 Bad Request`: Invalid input data.

### Check System Status
**GET** `/actuator/health`

## Docker Commands

| Action | Command |
| :--- | :--- |
| **Start DB** | `docker-compose up -d` |
| **Stop DB** | `docker-compose stop` |
| **Reset Data** | `docker-compose down -v` |

