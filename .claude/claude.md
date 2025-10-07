# mkrew2 - Blood Inventory Forecasting System

## Project Overview

**mkrew2** is a comprehensive forecasting system for predicting blood demand in Poland's 21 Regional Blood Donation and Blood Treatment Centers (RCKiK). The system uses machine learning to predict potential blood shortages and provides early warning alerts.

**Primary Goal:** Develop a forecasting system to predict blood demand across all RCKiK centers, enabling early detection of potential shortages.

**Academic Context:** Course project for 10xdevs 2.0

## Key Capabilities

- Automatic collection of blood inventory data from 21 RCKiK centers across Poland
- Historical tracking of 8 blood types (A+, A-, B+, B-, AB+, AB-, O+, O-)
- Machine learning forecasting using 4 models (ARIMA, Prophet, SARIMA, LSTM)
- Early warning system for potential blood shortages
- Dashboard with visualizations and forecasts (in development)

## Architecture

The project follows a microservices architecture with 5 main components:

```
┌──────────┐     HTTP      ┌──────────┐
│  RCKiK   │ ◄──────────── │ SCRAPER  │
│ websites │               │  :8080   │
└──────────┘               └─────┬────┘
                                 │ INSERT
                                 ▼
                           ┌──────────┐
                           │PostgreSQL│◄─────┐
                           │   DB     │      │
                           └─────┬────┘      │
                                 │ SELECT    │ INSERT
                                 ▼           │
┌──────────┐     REST      ┌──────────┐     │
│ FRONTEND │ ◄──────────►  │ BACKEND  │─────┘
│  :4321   │               │  :8081   │
└──────────┘               └─────┬────┘
                                 │ REST
                                 ▼
                           ┌──────────┐
                           │    ML    │
                           │  :5000   │
                           └──────────┘
```

### Components

1. **Database (`/db`)** - PostgreSQL with Liquibase migrations
2. **Scraper (`/scraper`)** - Spring Boot service for web scraping RCKiK websites
3. **ML Service (`/ml`)** - Python/Flask service with 4 forecasting models
4. **Backend (`/backend`)** - Spring Boot REST API
5. **Frontend (`/frontend`)** - Astro web application

## Technology Stack

| Component | Technologies | Port |
|-----------|-------------|------|
| Database | PostgreSQL 16 + Liquibase (YAML) | 5432 |
| Scraper | Spring Boot 3.4.1, Java 21, Jsoup, Quartz Scheduler | 8080 |
| ML Service | Python 3.11, Flask, ARIMA, Prophet, SARIMA, LSTM, TensorFlow | 5000 |
| Backend | Spring Boot 3.4.1, Java 21, Spring Security, JWT | 8081 |
| Frontend | Astro 5.14, React 19, Tailwind CSS 4 | 4321 |

## Directory Structure

```
mkrew2/
├── db/                    # ⚠️ PRIMARY database location (Liquibase)
│   └── changelog/
│       ├── db.changelog-master.yaml
│       └── changes/       # Migration files (001-005)
├── backend/              # REST API
│   ├── src/main/java/pl/mkrew/backend/
│   │   ├── domain/       # Entities, enums
│   │   ├── repository/   # Spring Data JPA
│   │   ├── service/      # Business logic
│   │   ├── controller/   # REST endpoints
│   │   ├── security/     # JWT + Spring Security
│   │   └── dto/          # Data Transfer Objects
│   └── build.gradle.kts
├── scraper/              # Web scraping service
│   ├── src/main/java/pl/mkrew/scraper/
│   │   ├── scraper/strategy/  # RCKiK-specific scrapers
│   │   ├── service/
│   │   └── scheduler/    # Quartz scheduled jobs
│   └── build.gradle.kts
├── ml/                   # Machine Learning service
│   ├── src/
│   │   ├── api/app.py    # Flask REST API
│   │   └── models/       # ML models (ARIMA, Prophet, SARIMA, LSTM)
│   └── requirements.txt
└── frontend/             # Web UI
    ├── src/
    │   ├── components/   # React/Astro components
    │   ├── pages/        # Astro pages (routing)
    │   └── layouts/
    └── package.json
```

## Database Schema

**Key Tables:**
- `rckik` - 21 Regional Blood Centers
- `blood_inventory_record` - Historical blood inventory states
- `scraping_log` - Web scraping logs
- `users` - System users (ADMIN, USER_DATA)
- `forecast_model` - ML models metadata
- `forecast_request` - Forecast requests
- `forecast_result` - Forecast results with confidence intervals

**Blood Types:** A+, A-, B+, B-, AB+, AB-, O+, O-

**Inventory Status Encoding:**
- CRITICALLY_LOW (1): < 1.5 days
- LOW (2): 1.5 - 2.5 days
- MEDIUM (3): 2.5 - 3.5 days
- SATISFACTORY (4): 3.5 - 4.5 days
- OPTIMAL (5): > 4.5 days

## Machine Learning Models

All 4 models are **fully implemented** in `/ml`:

1. **ARIMA(1,1,1)** - AutoRegressive Integrated Moving Average
   - Short-term forecasting (1-7 days)
   - Minimum data: 10 points
   - Fastest model

2. **Prophet** - Facebook Prophet
   - Medium/long-term forecasting (1-30 days)
   - Minimum data: 14 points
   - Handles seasonality, holidays, weekly patterns

3. **SARIMA(1,1,1)x(1,1,1,7)** - Seasonal ARIMA
   - Forecasting with weekly seasonality (1-14 days)
   - Minimum data: 14 points
   - Detects cyclical patterns

4. **LSTM** - Long Short-Term Memory (Deep Learning)
   - Complex non-linear patterns (1-7 days)
   - Minimum data: 27 points
   - Most accurate for complex patterns
   - Architecture: 2-layer LSTM (50→25 units) + Dropout

## API Endpoints

### Backend (Port 8081)

**Authentication:**
- `POST /api/auth/login` - User login (public)

**Blood Inventory:**
- `GET /api/blood-inventory/**` - Blood data (USER_DATA, ADMIN)

**Admin:**
- `POST /api/admin/scraper/trigger` - Trigger scraping (ADMIN)

**Forecasting:**
- `POST /api/forecast/create` - Create forecast (ADMIN)
- `GET /api/forecast/{id}` - Get forecast by ID (ADMIN)
- `GET /api/forecast/all` - Get all forecasts (ADMIN)
- `GET /api/forecast/rckik/{rckikId}` - Get forecasts for RCKiK (ADMIN)

### ML Service (Port 5000)

- `GET /health` - Health check
- `POST /api/forecast` - Generate forecast (supports all 4 models)
- `GET /api/models` - List available models with parameters

### Scraper (Port 8080) - 🔒 Secured with API Key

**⚠️ Important:** Scraper API is secured and only accessible with valid API key via `X-API-Key` header.

- `POST /api/scraper/trigger-all` - Trigger scraping for all RCKiK (requires API key)
- `POST /api/scraper/trigger/{rckikCode}` - Trigger scraping for specific RCKiK (requires API key)
- `GET /api/scraper/health` - Health check (public, no API key required)

## Authentication & Authorization

**Method:** JWT (Bearer tokens) with Spring Security

**Roles:**
- **ADMIN** - Full access (scraping, forecasts, user management)
- **USER_DATA** - Read-only access to blood inventory data

**Default Users:**
- Admin: `admin` / `admin123`
- User: `user` / `user123`

## Configuration

### Local Development

**Backend:** `backend/src/main/resources/application.properties`
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/mkrew
jwt.secret=your-256-bit-secret-key-change-this-in-production-mkrew-blood-inventory-system
scraper.service.url=http://localhost:8080
ml.service.url=http://localhost:5000
```

**Scraper:** Port 8080, connects to PostgreSQL at localhost:5432

**ML Service:** `.env` file in `/ml` directory
```env
PORT=5000
DEBUG=False
FLASK_ENV=production
```

### Docker Deployment

Services run in this order:
1. `postgres` (with healthcheck)
2. `liquibase` (runs migrations)
3. `scraper` + `ml` (parallel)
4. `backend` (waits for postgres + scraper + ml)
5. `frontend` (waits for backend)

**Network:** `mkrew-network` (bridge)

**Volumes:** `postgres_data` for database persistence

## Data Flow: Forecasting

1. Admin calls `POST /api/forecast/create` (Backend)
2. Backend fetches historical data from `blood_inventory_record`
3. Backend prepares ML request (`MLForecastRequest`)
4. Backend sends `POST http://ml:5000/api/forecast`
5. ML Service processes data, trains selected model
6. ML Service generates forecast with confidence intervals (95%)
7. ML Service returns `MLForecastResponse`
8. Backend saves results to `forecast_result` table
9. Backend returns `ForecastResponseDto` to client
10. Frontend displays forecasts (in development)

## Scraping Process

**Schedule:** Automated scraping at 8:00, 14:00, 20:00 daily (Quartz Scheduler)

**Strategy Pattern:** Each RCKiK has its own scraper strategy:
- `RzeszowScraperStrategy.java`
- `KrakowScraperStrategy.java`
- `WroclawScraperStrategy.java`
- (21 total centers)

**Data Collected:**
- Date/time of collection
- Blood type
- Inventory status (CRITICALLY_LOW to OPTIMAL)
- RCKiK source

## Security

### Scraper API Security

The Scraper service is secured with **two layers of protection**:

1. **API Key Authentication** (Application Layer)
   - All scraper endpoints (except `/health`) require `X-API-Key` header
   - Filter: `ApiKeyFilter.java` validates the API key
   - Configuration: `scraper.api.key` property (environment variable: `SCRAPER_API_KEY`)
   - Backend automatically includes API key in all requests to scraper

2. **Network Isolation** (Infrastructure Layer - Docker only)
   - Scraper port (8080) is NOT exposed to host machine in Docker
   - Only accessible within `mkrew-network` Docker network
   - Backend communicates with scraper via internal Docker DNS: `http://scraper:8080`
   - External requests cannot reach scraper directly

**Configuration:**
- Set `SCRAPER_API_KEY` environment variable or use default value
- Default: `change-this-secure-api-key-in-production-mkrew-scraper-2024`
- ⚠️ **Important:** Change the API key in production!

**Local Development:**
- Scraper is accessible at `http://localhost:8080` (port exposed)
- Still requires API key for all requests (except `/health`)

## Important Notes for Development

1. **Database Migrations:** Use Liquibase (NOT Flyway). YAML format in `/db/changelog/changes/`
2. **Primary DB Location:** `/db` is the main database directory, NOT `backend/src/main/resources/db`
3. **ML Service Status:** Fully implemented with all 4 models
4. **Backend-ML Communication:**
   - Local: `http://localhost:5000`
   - Docker: `http://ml:5000`
5. **Scraper Security:**
   - API Key required for all endpoints except `/health`
   - In Docker: network isolation (port not exposed to host)
6. **Build Tool:** Gradle (Kotlin DSL) for Java services
7. **Java Version:** 21 (required)
8. **Python Version:** 3.11+ (required for ML service)

## Running the Project

### With Docker Compose (Recommended)

```bash
docker-compose up -d --build
docker-compose logs -f
docker-compose down
```

### Local Development

1. Start PostgreSQL on localhost:5432
2. Run ML Service: `cd ml && python src/api/app.py`
3. Run Scraper: `cd scraper && ./gradlew bootRun`
4. Run Backend: `cd backend && ./gradlew bootRun`
5. Run Frontend: `cd frontend && npm install && npm run dev`

## Current Status

- ✅ **Database** - Complete (5 Liquibase migrations)
- ✅ **Scraper** - Complete (web scraping + scheduling)
- ✅ **ML Service** - Complete (4 models: ARIMA, Prophet, SARIMA, LSTM)
- ✅ **Backend** - Complete (REST API + forecast integration)
- 🔄 **Frontend** - In development (basic structure, needs forecast visualization)

## Research Methodology

Full research methodology documented in `metodologia.md`:
- Data collection strategies
- Seasonal pattern identification
- Risk factor analysis
- Model evaluation metrics
- Alert system design
- 12-24 month data collection period recommended

## Next Steps (TODO)

- [ ] Frontend forecast visualization dashboard
- [ ] Alert and notification system
- [ ] Admin dashboard
- [ ] Forecast accuracy metrics and monitoring
- [ ] Automatic model parameter optimization (auto-ARIMA)
- [ ] Batch prediction API
- [ ] Jupyter notebooks for experiments
- [ ] Model benchmarking
- [ ] Redis caching for forecasts
- [ ] Model persistence (save trained models)

## Regional Centers (21 RCKiK)

Białystok, Bydgoszcz, Gdańsk, Kalisz, Katowice, Kielce, Kraków, Lublin, Łódź, Olsztyn, Opole, Poznań, Racibórz, Radom, Rzeszów, Słupsk, Szczecin, Wałbrzych, Warszawa, Wrocław, Zielona Góra

## Additional Documentation

- `PROJECT_STRUCTURE.md` - Detailed project structure and implementation status
- `metodologia.md` - Complete research methodology
- `db/erd-diagram.drawio` - Database ERD diagram
- `ml/README.md` - ML service documentation

---

**Last Updated:** 2025-01-07
**Version:** 2.0
**Language:** Polish (documentation), English (code)
