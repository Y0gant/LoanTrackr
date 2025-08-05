# 💸 LoanTrackr

> **Enterprise-grade loan lifecycle management system built with Java Spring Boot**

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13%2B-blue?style=flat-square&logo=postgresql)](https://postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7%2B-red?style=flat-square&logo=redis)](https://redis.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-informational?style=flat-square&logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Security](#-security)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**LoanTrackr** is a comprehensive, production-ready loan management system designed for modern financial institutions.
Built with Java Spring Boot, it provides end-to-end loan lifecycle management from application to closure, featuring
robust security, role-based access control, and seamless integration capabilities.

### 🏦 Perfect for:

- **NBFCs & Microfinance Institutions** - Complete loan portfolio management
- **P2P Lending Platforms** - Borrower-lender matchmaking and tracking
- **Fintech Startups** - Quick-to-market credit platform foundation
- **Enterprise Projects** - Showcase of modern Java backend architecture

---

## ✨ Features

### 🔐 Authentication & Authorization

- **JWT-based authentication** with refresh token support
- **Role-based access control** (Admin, Borrower, Lender)
- **OTP verification** for secure user registration
- **Session management** with Redis caching

### 👥 User Management

- **Admin Dashboard** - Complete system oversight and user management
- **Lender Onboarding** - Document verification (PAN, GST, RBI License)
- **Borrower KYC** - Aadhaar and PAN validation workflow
- **Profile Management** - Secure document storage and updates

### 💰 Loan Lifecycle Management

- **Application Processing** - Digital loan application with document upload
- **Credit Assessment** - Configurable approval workflows
- **Loan Disbursement** - Automated fund transfer integration
- **EMI Management** - Auto-calculated repayment schedules
- **Payment Processing** - Multiple payment gateway support
- **Recovery Management** - Automated reminder and collection workflows

### 🔧 Technical Features

- **RESTful API Design** - Clean, documented endpoints
- **Database Transactions** - ACID compliance with PostgreSQL
- **Caching Layer** - Redis for performance optimization
- **Email Notifications** - SMTP-based communication system
- **File Management** - Secure document storage and retrieval
- **Audit Logging** - Complete transaction history tracking

---

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │   LoanTrackr    │
│   (Web/Mobile)  │────│                 │────│   Backend       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐             │
                       │     Redis       │─────────────┤
                       │   (Caching)     │             │
                       └─────────────────┘             │
                                                       │
                       ┌─────────────────┐             │
                       │   PostgreSQL    │─────────────┘
                       │   (Database)    │
                       └─────────────────┘
```

### Key Components:

- **Controller Layer** - REST API endpoints and request handling
- **Service Layer** - Business logic and transaction management
- **Repository Layer** - Data access and database operations
- **Security Layer** - JWT authentication and authorization
- **Configuration Layer** - Application settings and external integrations

---

## 🛠️ Tech Stack

| Category          | Technology      | Version | Purpose                        |
|-------------------|-----------------|---------|--------------------------------|
| **Backend**       | Java            | 17+     | Core programming language      |
| **Framework**     | Spring Boot     | 3.x     | Application framework          |
| **Security**      | Spring Security | 6.x     | Authentication & authorization |
| **Database**      | PostgreSQL      | 13+     | Primary data storage           |
| **Caching**       | Redis           | 7+      | Session management & caching   |
| **Build Tool**    | Maven           | 3.8+    | Dependency management          |
| **Documentation** | Swagger/OpenAPI | 3.x     | API documentation              |
| **Testing**       | JUnit 5         | 5.x     | Unit testing framework         |
| **Email**         | JavaMail        | Latest  | SMTP email service             |

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:

```bash
# Check Java version (17+ required)
java -version

# Check Maven version (3.8+ required)
mvn -version

# Check PostgreSQL (13+ required)
psql --version

# Check Redis (7+ required)
redis-server --version
```

### 📥 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Y0gant/loantrackr.git
   cd loantrackr
   ```

2. **Set up PostgreSQL database**
   ```sql
   -- Connect to PostgreSQL as superuser
   CREATE DATABASE loantrackr;
   CREATE USER loantrackr_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE loantrackr TO loantrackr_user;
   ```

3. **Start Redis server**
   ```bash
   # On Linux/macOS
   redis-server
   
   # On Windows (if using Redis for Windows)
   redis-server.exe
   ```

4. **Configure application properties**
   ```bash
   # Copy example configuration
   cp src/main/resources/application-example.yml src/main/resources/application.yml
   
   # Create secrets file (DO NOT commit this file)
   touch src/main/resources/application-secret.yml
   ```

5. **Build and run the application**
   ```bash
   # Clean and compile
   ./mvnw clean compile
   
   # Run tests
   ./mvnw test
   
   # Start the application
   ./mvnw spring-boot:run
   ```

6. **Verify installation**
   ```bash
   # Check if application is running
   curl http://localhost:8080/actuator/health
   
   # Access Swagger UI
   open http://localhost:8080/swagger-ui.html
   ```

---

## ⚙️ Configuration

### Required Configuration Files

#### `application.yml` (Main Configuration)

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  profiles:
    active: dev
    include: secret

  datasource:
    url: jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  redis:
    host: ${REDIS_DATABASE_HOST}
    port: ${REDIS_DATABASE_PORT}
    timeout: 2000ms
    pool:
      max-active: 8
      max-idle: 8
      min-idle: 0

  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

logging:
  level:
    com.loantrackr: DEBUG
    org.springframework.security: DEBUG
```

#### `application-secret.yml` (Environment Secrets)

```yaml
DATABASE_HOST: localhost
DATABASE_PORT: 5432
DATABASE_NAME: loantrackr
DATABASE_USERNAME: loantrackr_user
DATABASE_PASSWORD: your_secure_password

REDIS_DATABASE_HOST: localhost
REDIS_DATABASE_PORT: 6379

SMTP_HOST: smtp.gmail.com
SMTP_PORT: 587
SMTP_USERNAME: your-email@gmail.com
SMTP_PASSWORD: your-app-password

JWT_SECRET_KEY: your-256-bit-secret-key-here
JWT_EXPIRATION: 86400000  # 24 hours in milliseconds

BOOTSTRAP_EMAIL: admin@loantrackr.com
BOOTSTRAP_PASSWORD: SecureAdmin@123

# File upload settings
FILE_UPLOAD_DIR: ./uploads
MAX_FILE_SIZE: 10MB
```

### Environment Variables (Production)

For production deployment, set these as environment variables:

```bash
export DATABASE_HOST=your-db-host
export DATABASE_PASSWORD=your-secure-password
export JWT_SECRET_KEY=your-production-secret
# ... other variables
```

---

## 📚 API Documentation

### Access Swagger UI

Once the application is running, visit: `http://localhost:8080/swagger-ui.html`

### Key API Endpoints

#### Authentication

- `POST /auth/register` - User registration with OTP verification
- `POST /auth/login` - User login with JWT token
- `POST /auth/refresh` - Refresh JWT token
- `POST /auth/logout` - User logout

#### User Management

- `GET /users/profile` - Get user profile
- `PUT /users/profile` - Update user profile
- `POST /users/kyc` - Submit KYC documents
- `GET /admin/users` - List all users (Admin only)

#### Loan Management

- `POST /loans/apply` - Submit loan application
- `GET /loans` - Get user's loans
- `PUT /loans/{id}/approve` - Approve loan (Lender/Admin)
- `POST /loans/{id}/disburse` - Disburse loan funds
- `GET /loans/{id}/schedule` - Get EMI schedule
- `POST /loans/{id}/payment` - Make EMI payment

### Sample API Response

```json
{
  "success": true,
  "message": "Loan application submitted successfully",
  "data": {
    "loanId": "LN001234567890",
    "amount": 50000.00,
    "tenure": 12,
    "interestRate": 12.5,
    "emiAmount": 4458.33,
    "status": "PENDING_APPROVAL"
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

---

### Database Migration

The application uses Hibernate DDL auto-update. For production environments, consider using Flyway or Liquibase for
version-controlled database migrations.

---

## 🔒 Security

### JWT Implementation

- **Access Tokens**: Short-lived (1 hour) for API access

### Role-Based Access Control

```java

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOnlyEndpoint() { ...}

@PreAuthorize("hasRole('BORROWER') or hasRole('ADMIN')")
public ResponseEntity<?> borrowerOrAdminEndpoint() { ...}
```

### Data Protection

- Password hashing using BCrypt
- Sensitive data encryption at rest
- Audit logging for compliance
- Secure file upload validation

---

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=LoanServiceTest

```

### Test Categories

- **Unit Tests**: Service layer business logic
- **Integration Tests**: Database and external service integration
- **Security Tests**: Authentication and authorization
- **API Tests**: REST endpoint validation

---

## 🚀 Deployment

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/loantrackr-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build Docker image
docker build -t loantrackr:latest .

# Run with Docker Compose
docker-compose up -d
```

## 🤝 Contributing

We welcome contributions!

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards

- Follow Java coding conventions
- Write comprehensive tests
- Update documentation
- Ensure all tests pass

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

- **Email**: yogantfaye7@gmail.com

---

