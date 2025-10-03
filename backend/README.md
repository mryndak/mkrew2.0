# Backend API - Blood Inventory Tracking

## Overview
REST API backend for blood inventory tracking system with role-based access control.

## Tech Stack
- Java 21
- Spring Boot 3.4.1
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Gradle

## Quick Start

```bash
# Run application
./gradlew bootRun
```

Server starts on port 8081.

## Default Users

**Admin**: username=`admin`, password=`admin123` (Role: ADMIN)
**User**: username=`user`, password=`user123` (Role: USER_DATA)

## API Endpoints

### Auth
- `POST /api/auth/login` - Get JWT token

### Blood Inventory (USER_DATA role)
- `GET /api/blood-inventory/current` - Current status all RCKiK
- `GET /api/blood-inventory/current/{rckikCode}` - Current for one RCKiK
- `GET /api/blood-inventory/history/{rckikCode}?bloodType={type}&period={days}` - History (1,7,30,90,365 days)

### Admin (ADMIN role)
- `POST /api/admin/scraper/trigger-all` - Trigger all scraping
- `POST /api/admin/scraper/trigger/{rckikCode}` - Trigger one RCKiK

## Implementation Status

✅ Project structure created
✅ Dependencies configured
✅ User entity and migrations
✅ Database schema updated
⏳ Security configuration (JWT) - TODO
⏳ Controllers and services - TODO
⏳ Scraper REST client - TODO

See ARCHITECTURE.md for detailed design.
