# Backend Architecture

## Stack
- Java 21
- Spring Boot 3.4.1
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Gradle

## API Endpoints

### Authentication
```
POST /api/auth/login - Login (returns JWT token)
POST /api/auth/register - Register new user (ADMIN only)
```

### Blood Inventory (Role: USER_DATA)
```
GET /api/blood-inventory/current - Get current blood status for all RCKiK
GET /api/blood-inventory/current/{rckikCode} - Get current status for specific RCKiK
GET /api/blood-inventory/history/{rckikCode}?bloodType={type}&period={days} - Get history
  - period: 1, 7, 30, 90, 365 days
```

### Admin (Role: ADMIN)
```
POST /api/admin/scraper/trigger-all - Trigger scraping for all RCKiK
POST /api/admin/scraper/trigger/{rckikCode} - Trigger for specific RCKiK
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_login TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
```

### Reuses existing tables from scraper DB:
- rckik
- blood_inventory_record
- scraping_log

## Security
- JWT-based authentication
- Role-based access control (ADMIN, USER_DATA)
- Password encryption with BCrypt

## Configuration
- Database URL: jdbc:postgresql://localhost:5432/mkrew
- Server Port: 8081 (to avoid conflict with scraper on 8080)
