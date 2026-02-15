# E-Commerce Microservices Project

## 📋 Project Overview

This project implements a complete microservices architecture for an e-commerce inventory and order management system using Spring Boot. The system consists of two microservices:

1. **Inventory Service** (Port 8081) - Manages product inventory with batch tracking and expiry dates
2. **Order Service** (Port 8080) - Handles order placement with real-time inventory validation

## 🏗️ Architecture

```
┌─────────────────┐         REST API        ┌──────────────────┐
│                 │ ───────────────────────> │                  │
│  Order Service  │                          │ Inventory Service│
│   (Port 8080)   │ <─────────────────────── │   (Port 8081)    │
│                 │    Inventory Check       │                  │
└─────────────────┘                          └──────────────────┘
       │                                              │
       │ H2 Database                                  │ H2 Database
       │ (orderdb)                                    │ (inventorydb)
       ▼                                              ▼
```

## ✨ Key Features

### Factory Design Pattern
- **InventoryHandler Interface**: Allows multiple inventory handling strategies
- **FifoInventoryHandler**: Current implementation using First-In-First-Out
- **InventoryHandlerFactory**: Creates and manages handlers
- **Extensibility**: Easy to add LIFO, Location-Based, or Priority handlers

### Technical Stack
- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA** - ORM for database operations
- **H2 Database** - In-memory database
- **Liquibase** - Database migration and data loading
- **Lombok** - Reduces boilerplate code
- **WebClient** - Inter-service communication
- **Swagger/OpenAPI** - API documentation
- **JUnit 5 & Mockito** - Testing

### Business Logic
- **FIFO Inventory Management**: Batches consumed by expiry date (oldest first)
- **Real-time Availability**: Orders check inventory before placement
- **Atomic Transactions**: All-or-nothing inventory updates
- **Batch Tracking**: Each order records which batches were used

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Terminal/Command Prompt

### Step 1: Clone/Extract Project
```bash
cd ecommerce-microservices
```

### Step 2: Build Both Services
```bash
# Build parent and all modules
mvn clean install
```

### Step 3: Start Inventory Service
```bash
# Terminal 1
cd inventory-service
mvn spring-boot:run
```

Wait for "Started InventoryServiceApplication" message.

### Step 4: Start Order Service
```bash
# Terminal 2 (new terminal)
cd order-service
mvn spring-boot:run
```

Wait for "Started OrderServiceApplication" message.

### Step 5: Verify Services are Running

**Inventory Service:**
- API: http://localhost:8081/inventory/1001
- Swagger UI: http://localhost:8081/swagger-ui.html
- H2 Console: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:inventorydb`
  - Username: `sa`
  - Password: (leave empty)

**Order Service:**
- API: http://localhost:8080/order
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:orderdb`
  - Username: `sa`
  - Password: (leave empty)

## 📝 API Documentation

### Inventory Service Endpoints

#### 1. Get Inventory (GET)
```
GET http://localhost:8081/inventory/{productId}
```

**Example Request:**
```bash
curl http://localhost:8081/inventory/1001
```

**Example Response:**
```json
{
  "productId": 1001,
  "productName": "Laptop",
  "batches": [
    {
      "batchId": 1,
      "quantity": 68,
      "expiryDate": "2026-06-25"
    }
  ],
  "totalQuantity": 68
}
```

#### 2. Update Inventory (POST)
```
POST http://localhost:8081/inventory/update
Content-Type: application/json
```

**Example Request:**
```bash
curl -X POST http://localhost:8081/inventory/update \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1001,
    "totalQuantity": 10,
    "batchUpdates": [
      {"batchId": 1, "quantityDeducted": 10}
    ]
  }'
```

**Example Response:**
```json
{
  "message": "Inventory updated successfully for product 1001"
}
```

### Order Service Endpoints

#### Place Order (POST)
```
POST http://localhost:8080/order
Content-Type: application/json
```

**Example Request:**
```bash
curl -X POST http://localhost:8080/order \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1002,
    "quantity": 3
  }'
```

**Example Response:**
```json
{
  "orderId": 11,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9, 10],
  "message": "Order placed. Inventory reserved.",
  "orderDate": "2026-02-12T10:30:00"
}
```

## 🧪 Testing

### Run All Tests
```bash
# From project root
mvn test
```

### Run Tests for Specific Service
```bash
# Inventory Service tests
cd inventory-service
mvn test

# Order Service tests
cd order-service
mvn test
```

### Test Coverage
- **Unit Tests**: Service layer with Mockito
- **Integration Tests**: Full stack with @SpringBootTest
- **Component Tests**: REST endpoints with MockMvc

### Test Files
- `InventoryServiceTest.java` - Unit tests for inventory service
- `InventoryControllerIntegrationTest.java` - API integration tests
- `OrderServiceTest.java` - Unit tests for order service

## 📊 Sample Data

### Inventory Batches (Pre-loaded via Liquibase)
| Batch ID | Product ID | Product Name | Quantity | Expiry Date |
|----------|-----------|--------------|----------|-------------|
| 1 | 1001 | Laptop | 68 | 2026-06-25 |
| 2 | 1005 | Smartwatch | 52 | 2026-05-30 |
| 5 | 1005 | Smartwatch | 39 | 2026-03-31 |
| 9 | 1002 | Smartphone | 29 | 2026-05-31 |
| 10 | 1002 | Smartphone | 83 | 2026-11-15 |

### Orders (Pre-loaded via Liquibase)
| Order ID | Product ID | Product Name | Quantity | Status |
|----------|-----------|--------------|----------|---------|
| 1 | 1005 | Smartwatch | 10 | DELIVERED |
| 2 | 1003 | Tablet | 10 | PLACED |
| 9 | 1001 | Laptop | 10 | DELIVERED |

## 🏭 Design Patterns & Architecture

### 1. Factory Pattern (Inventory Service)
```
InventoryHandler (interface)
    ↑
    └── FifoInventoryHandler (concrete)
    └── [Future: LifoInventoryHandler]
    └── [Future: LocationBasedHandler]
    
InventoryHandlerFactory
    └── Creates appropriate handler
    └── Auto-discovers all handlers via Spring
```

**Benefits:**
- Easy to add new inventory strategies
- Loose coupling between service and implementation
- Follows Open/Closed Principle

### 2. Layered Architecture
```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database Layer (H2)
```

### 3. Microservices Communication
- **REST API**: Order Service → Inventory Service
- **WebClient**: Non-blocking HTTP client
- **Loose Coupling**: Services are independent

## 🔧 Configuration

### Inventory Service (application.yml)
```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:h2:mem:inventorydb
inventory:
  handler:
    default: FIFO  # Can be changed to other handlers
```

### Order Service (application.yml)
```yaml
server:
  port: 8080
inventory:
  service:
    url: http://localhost:8081  # Inventory Service URL
```

## 🐛 Troubleshooting

### Port Already in Use
If you see "Port 8080 is already in use":
```bash
# On Linux/Mac
lsof -i :8080
kill -9 <PID>

# On Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Services Can't Communicate
1. Ensure Inventory Service is started first
2. Check logs for connection errors
3. Verify URL in `order-service/application.yml`

### Database Issues
1. Check H2 console at /h2-console
2. Verify Liquibase changelogs ran successfully
3. Check logs for migration errors

## 📚 Code Structure

```
ecommerce-microservices/
├── pom.xml                          # Parent POM
├── inventory-service/
│   ├── src/main/java/
│   │   └── com/ecommerce/inventory/
│   │       ├── controller/          # REST endpoints
│   │       ├── service/             # Business logic
│   │       ├── factory/             # Factory pattern
│   │       ├── repository/          # Data access
│   │       ├── model/               # Entities
│   │       └── dto/                 # Data transfer objects
│   ├── src/main/resources/
│   │   ├── application.yml          # Configuration
│   │   └── db/changelog/            # Liquibase migrations
│   └── src/test/java/               # Tests
├── order-service/
│   ├── src/main/java/
│   │   └── com/ecommerce/order/
│   │       ├── controller/          # REST endpoints
│   │       ├── service/             # Business logic
│   │       ├── client/              # External API clients
│   │       ├── repository/          # Data access
│   │       ├── model/               # Entities
│   │       └── dto/                 # Data transfer objects
│   ├── src/main/resources/
│   │   ├── application.yml          # Configuration
│   │   └── db/changelog/            # Liquibase migrations
│   └── src/test/java/               # Tests
└── README.md                        # This file
```

## 🎯 Assignment Requirements Checklist

✅ **Microservices**
- [x] Inventory Service with batch management
- [x] Order Service with inventory integration

✅ **Database**
- [x] H2 in-memory database
- [x] Spring Data JPA
- [x] Liquibase for schema and data loading

✅ **Factory Pattern**
- [x] InventoryHandler interface
- [x] FifoInventoryHandler implementation
- [x] InventoryHandlerFactory for extensibility

✅ **REST APIs**
- [x] GET /inventory/{productId}
- [x] POST /inventory/update
- [x] POST /order

✅ **Inter-service Communication**
- [x] WebClient for REST calls
- [x] Order Service → Inventory Service

✅ **Testing**
- [x] JUnit 5 unit tests
- [x] Mockito for mocking
- [x] @SpringBootTest integration tests
- [x] REST endpoint testing

✅ **Additional Features**
- [x] Lombok for boilerplate reduction
- [x] Swagger/OpenAPI documentation
- [x] Validation with Bean Validation
- [x] Comprehensive logging
- [x] Transaction management

## 🚀 Next Steps / Future Enhancements

1. **Add Authentication**: JWT tokens for API security
2. **Add More Handlers**: LIFO, LocationBased inventory strategies
3. **Order Cancellation**: Release inventory when orders cancelled
4. **Notifications**: Email/SMS on order placement
5. **Metrics**: Prometheus for monitoring
6. **Docker**: Containerize both services
7. **Service Discovery**: Eureka for dynamic service discovery
8. **API Gateway**: Spring Cloud Gateway for routing

## 📞 Support

For issues or questions:
1. Check H2 console for data verification
2. Review application logs
3. Verify Swagger documentation
4. Check test cases for examples

## 📄 License

This is an educational project for learning Spring Boot microservices architecture.

---

**Built with ❤️ using Spring Boot, Java 17, and modern microservices patterns**
