# Struktura projektu mkrew2

## 📋 Cel projektu

**Główny cel:** Opracowanie systemu prognostycznego przewidującego zapotrzebowanie na krew w Regionalnych Centrach Krwiodawstwa i Krwiolecznictwa (RCKiK) w Polsce, umożliwiającego wczesne wykrywanie potencjalnych niedoborów.

### Kluczowe funkcjonalności:
- ✅ Automatyczne zbieranie danych o stanach magazynowych krwi z 21 RCKiK w Polsce
- ✅ Przechowywanie historycznych danych o 8 grupach krwi (A+, A-, B+, B-, AB+, AB-, O+, O-)
- ✅ Prognozowanie niedoborów krwi przy użyciu modeli ML (ARIMA, Prophet)
- ✅ System wczesnego ostrzegania o potencjalnych niedoborach
- ✅ Dashboard z wizualizacjami i prognozami

## 🏗️ Architektura projektu

```
mkrew2/
├── /db                 # ⚠️ Liquibase migrations - GŁÓWNY katalog bazy danych!
├── /backend            # REST API (Spring Boot)
├── /scraper            # Serwis scrapujący dane RCKiK
├── /frontend           # Aplikacja webowa (Astro)
└── /ml                 # Modele uczenia maszynowego (planowany)
```

## 📁 Szczegółowa struktura katalogów

### 📂 `/db` - Baza danych (Liquibase)
**⚠️ WAŻNE: To jest JEDYNE miejsce na migracje bazy danych!**

```
db/
├── changelog/
│   ├── db.changelog-master.yaml          # Master changelog
│   └── changes/
│       ├── 001-create-rckik-table.yaml
│       ├── 002-create-blood-inventory-record-table.yaml
│       ├── 003-create-scraping-log-table.yaml
│       ├── 004-create-users-table.yaml
│       └── 005-create-forecast-tables.yaml
└── erd-diagram.drawio
```

**Technologia:** Liquibase (NIE Flyway!)
**Format:** YAML changesets
**Baza danych:** PostgreSQL

**Tabele:**
- `rckik` - Regionalne Centra Krwiodawstwa (21 centrów)
- `blood_inventory_record` - Historyczne stany magazynowe krwi
- `scraping_log` - Logi scrapingu danych
- `users` - Użytkownicy systemu (ADMIN, USER_DATA)
- `forecast_model` - Modele ML (ARIMA, Prophet, SARIMA, LSTM)
- `forecast_request` - Żądania prognozy
- `forecast_result` - Wyniki prognoz z interwałami ufności

### 📂 `/backend` - Backend API
```
backend/
├── src/main/java/pl/mkrew/backend/
│   ├── domain/
│   │   ├── entity/          # JPA entities
│   │   └── enums/           # BloodType, InventoryStatus, UserRole, ForecastModelType
│   ├── repository/          # Spring Data JPA repositories
│   ├── service/             # Business logic + ML service client
│   ├── controller/          # REST endpoints
│   ├── security/            # JWT + Spring Security
│   ├── dto/                 # Data Transfer Objects
│   └── config/              # Configuration classes
└── build.gradle.kts
```

**Technologia:**
- Spring Boot 3.4.1
- Java 21
- Gradle
- PostgreSQL
- JWT Authentication
- Spring Security

**Port:** 8081

**Kluczowe endpointy:**
- `POST /api/auth/login` - Logowanie (public)
- `GET /api/blood-inventory/**` - Dane o krwi (USER_DATA, ADMIN)
- `POST /api/admin/scraper/trigger` - Trigger scrapingu (ADMIN)
- `POST /api/forecast/create` - Uruchom prognozę (ADMIN)
- `GET /api/forecast/{id}` - Pobierz prognozę (ADMIN)

**Użytkownicy domyślni:**
- Admin: `admin` / `admin123` (rola: ADMIN)
- User: `user` / `user123` (rola: USER_DATA)

### 📂 `/scraper` - Web Scraper
```
scraper/
├── src/main/java/pl/mkrew/scraper/
│   ├── domain/entity/       # JPA entities (RCKiK, BloodInventoryRecord, ScrapingLog)
│   ├── scraper/
│   │   ├── strategy/        # Strategie scrapingu dla różnych RCKiK
│   │   │   ├── RzeszowScraperStrategy.java
│   │   │   ├── KrakowScraperStrategy.java
│   │   │   └── WroclawScraperStrategy.java
│   │   └── dto/
│   ├── service/             # ScraperService
│   ├── scheduler/           # Quartz scheduled jobs
│   └── controller/          # REST endpoints do manualnego triggera
└── build.gradle.kts
```

**Technologia:**
- Spring Boot
- Java 21
- Jsoup (web scraping)
- Quartz Scheduler
- PostgreSQL

**Port:** 8080

**Funkcjonalność:**
- Automatyczne zbieranie danych ze stron RCKiK (cron: 8:00, 14:00, 20:00)
- Parsowanie HTML dla każdego centrum (osobne strategie)
- Zapis stanów magazynowych do bazy danych
- Logowanie statusu scrapingu

**Endpointy:**
- `POST /api/scraper/trigger` - Manual trigger scrapingu
- `GET /api/scraper/status` - Status ostatniego scrapingu

### 📂 `/frontend` - Frontend (Astro)
```
frontend/
├── src/
│   ├── components/          # React/Astro components
│   ├── layouts/             # Layout templates
│   ├── pages/               # Astro pages (routing)
│   └── styles/              # CSS/Tailwind
├── public/                  # Static assets
└── package.json
```

**Technologia:**
- Astro framework
- React (dla komponentów interaktywnych)
- Tailwind CSS
- TypeScript

**Port:** (do ustalenia)

**Planowane funkcje:**
- Dashboard z mapą Polski i stanami RCKiK
- Wizualizacje szeregów czasowych
- Wyświetlanie prognoz ML
- System alertów o niedoborach
- Panel administratora

### 📂 `/ml` - Machine Learning (planowany)
```
ml/
├── models/                  # Wytrenowane modele
├── notebooks/               # Jupyter notebooks (eksperymentowanie)
├── src/
│   ├── preprocessing/       # Przygotowanie danych
│   ├── training/            # Trenowanie modeli
│   └── api/                 # REST API dla prognoz
├── requirements.txt
└── Dockerfile
```

**Technologia (planowana):**
- Python 3.10+
- Flask/FastAPI (REST API)
- statsmodels (ARIMA)
- Prophet (Facebook)
- scikit-learn
- pandas, numpy

**Port:** 5000

**Modele do implementacji:**
1. **ARIMA** - AutoRegressive Integrated Moving Average (priorytet 1)
2. **Prophet** - Facebook's forecasting tool (priorytet 2)
3. **SARIMA** - Seasonal ARIMA (przyszłość)
4. **LSTM** - Deep learning (przyszłość)

**API endpoints (planowane):**
- `POST /api/forecast` - Utwórz prognozę
  - Input: dane historyczne + parametry modelu
  - Output: prognozy z interwałami ufności

## 🔄 Przepływ danych

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
│  Astro   │               │  :8081   │
└──────────┘               └─────┬────┘
                                 │ REST
                                 ▼
                           ┌──────────┐
                           │    ML    │
                           │  :5000   │
                           └──────────┘
```

### Szczegółowy flow:
1. **Scraper** → Pobiera dane ze stron RCKiK (codziennie 8:00, 14:00, 20:00)
2. **Scraper** → Zapisuje do `blood_inventory_record` w PostgreSQL
3. **Backend** → Udostępnia dane przez REST API
4. **Frontend** → Wyświetla dane użytkownikowi
5. **Admin** → Wywołuje prognozę przez `/api/forecast/create`
6. **Backend** → Pobiera dane historyczne z bazy
7. **Backend** → Wysyła request do ML service
8. **ML Service** → Generuje prognozę (ARIMA/Prophet)
9. **Backend** → Zapisuje wyniki do `forecast_result`
10. **Frontend** → Wyświetla prognozy

## 🔐 Bezpieczeństwo

### Autentykacja i autoryzacja:
- **JWT tokens** (Bearer authentication)
- **Spring Security** + BCrypt password encoding
- **Role-based access control (RBAC)**

### Role użytkowników:
1. **ADMIN**
   - Pełny dostęp do wszystkich endpointów
   - Może uruchamiać scraping i prognozy
   - Zarządzanie użytkownikami

2. **USER_DATA**
   - Dostęp tylko do odczytu danych o krwi
   - Brak dostępu do prognoz i administracji

## 🗄️ Konfiguracja

### Backend (`application.properties`):
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/mkrew
jwt.secret=your-256-bit-secret-key-change-this-in-production
scraper.service.url=http://localhost:8080
ml.service.url=http://localhost:5000
```

### Scraper (`application.properties`):
```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/mkrew
```

### Docker Compose:
```yaml
services:
  - postgres:5432 (baza danych)
  - backend:8081 (API)
  - scraper:8080 (scraping service)
  - frontend (do skonfigurowania)
```

## 🚀 Uruchomienie projektu

### Wymagania:
- Java 21
- PostgreSQL 15+
- Gradle 8+
- Node.js 18+ (dla frontendu)
- Python 3.10+ (dla ML, w przyszłości)

### Kroki:
1. Uruchom PostgreSQL
2. Uruchom migracje Liquibase (automatycznie przy starcie backendu/scrapera)
3. Uruchom scraper: `cd scraper && ./gradlew bootRun`
4. Uruchom backend: `cd backend && ./gradlew bootRun`
5. Uruchom frontend: `cd frontend && npm run dev`
6. (Przyszłość) Uruchom ML service: `cd ml && python app.py`

## 📊 Modele ML - Szczegóły

### ARIMA (priorytet 1):
- **Zastosowanie:** Prognozowanie stanów magazynowych krwi
- **Parametry:** p, d, q (zapisane w `forecast_model.model_parameters`)
- **Horyzont:** 1-7 dni (krótkoterminowy)
- **Input:** Szereg czasowy stanów dla danej grupy krwi w danym RCKiK
- **Output:** Prognoza + przedziały ufności

### Prophet (priorytet 2):
- **Zastosowanie:** Długoterminowe prognozy z sezonowością
- **Uwzględnia:** Święta, weekendy, wzorce roczne
- **Horyzont:** 1-4 tygodnie (średnioterminowy)

### System alertów (planowany):
- **Poziom 1 (Info):** Spadek do stanu średniego w ciągu 7 dni
- **Poziom 2 (Warning):** Spadek do stanu niskiego w ciągu 7 dni
- **Poziom 3 (Critical):** Spadek do stanu niskiego w ciągu 3 dni

## 📝 Dodatkowe informacje

### RCKiK (21 centrów):
Białystok, Bydgoszcz, Gdańsk, Kalisz, Katowice, Kielce, Kraków, Lublin, Łódź, Olsztyn, Opole, Poznań, Racibórz, Radom, Rzeszów, Słupsk, Szczecin, Wałbrzych, Warszawa, Wrocław, Zielona Góra

### Grupy krwi (8 typów):
A+, A-, B+, B-, AB+, AB-, O+, O-

### Kodowanie stanów magazynowych:
- `CRITICALLY_LOW` = Stan krytycznie niski
- `LOW` = Stan niski
- `MEDIUM` = Stan średni
- `SATISFACTORY` = Stan zadowalający
- `OPTIMAL` = Stan optymalny

## 🔧 Technologie - Podsumowanie

| Komponent | Technologia | Port | Cel |
|-----------|-------------|------|-----|
| Database | PostgreSQL + Liquibase | 5432 | Przechowywanie danych |
| Backend | Spring Boot 3.4 + Java 21 | 8081 | REST API + logika biznesowa |
| Scraper | Spring Boot + Jsoup + Quartz | 8080 | Web scraping RCKiK |
| Frontend | Astro + React + Tailwind | TBD | UI/Dashboard |
| ML Service | Python + Flask/FastAPI + Prophet/ARIMA | 5000 | Prognozy ML |

## 📚 Dokumentacja dodatkowa

- `metodologia.md` - Pełna metodologia badawcza projektu
- `db/erd-diagram.drawio` - Diagram ERD bazy danych
- `backend/ARCHITECTURE.md` - Architektura backendu (jeśli istnieje)
- `backend/IMPLEMENTATION_GUIDE.md` - Przewodnik implementacji

## ⚠️ Ważne uwagi dla AI/Asystentów

1. **Migracje bazy danych:** Używamy Liquibase (NIE Flyway!), pliki YAML w `/db/changelog/changes/`
2. **Struktura katalogów:** `/db` jest GŁÓWNYM miejscem dla bazy, NIE `backend/src/main/resources/db`
3. **Projekt jest w trakcie rozwoju:** Moduł `/ml` nie istnieje jeszcze
4. **Testowe dane:** Admin user już utworzony w migracji 004
5. **API prognozy:** Backend gotowy, czeka na implementację ML service w Pythonie

---

**Ostatnia aktualizacja:** 2025-10-03
**Status projektu:** Backend + Scraper działają, Frontend + ML w trakcie rozwoju
