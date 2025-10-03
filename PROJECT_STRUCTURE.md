# Struktura projektu mkrew2

## 📋 Cel projektu

**Główny cel:** Opracowanie systemu prognostycznego przewidującego zapotrzebowanie na krew w Regionalnych Centrach Krwiodawstwa i Krwiolecznictwa (RCKiK) w Polsce, umożliwiającego wczesne wykrywanie potencjalnych niedoborów.

### Kluczowe funkcjonalności:
- ✅ Automatyczne zbieranie danych o stanach magazynowych krwi z 21 RCKiK w Polsce
- ✅ Przechowywanie historycznych danych o 8 grupach krwi (A+, A-, B+, B-, AB+, AB-, O+, O-)
- ✅ Prognozowanie niedoborów krwi przy użyciu modeli ML (ARIMA, Prophet)
- ✅ System wczesnego ostrzegania o potencjalnych niedoborach
- 🔄 Dashboard z wizualizacjami i prognozami (w trakcie)

## 🏗️ Architektura projektu

```
mkrew2/
├── /db                 # ⚠️ Liquibase migrations - GŁÓWNY katalog bazy danych!
├── /backend            # REST API (Spring Boot)
├── /scraper            # Serwis scrapujący dane RCKiK
├── /frontend           # Aplikacja webowa (Astro)
└── /ml                 # ✅ Modele uczenia maszynowego (ZAIMPLEMENTOWANY!)
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
│       └── 005-create-forecast-tables.yaml    # ✅ Nowa migracja dla prognoz
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
│   │   │   ├── User.java
│   │   │   ├── RCKiK.java
│   │   │   ├── BloodInventoryRecord.java
│   │   │   ├── ForecastModel.java          # ✅ Nowa encja
│   │   │   ├── ForecastRequest.java        # ✅ Nowa encja
│   │   │   └── ForecastResult.java         # ✅ Nowa encja
│   │   └── enums/
│   │       ├── BloodType.java
│   │       ├── InventoryStatus.java
│   │       ├── UserRole.java
│   │       ├── ForecastModelType.java      # ✅ Nowy enum
│   │       └── ForecastStatus.java         # ✅ Nowy enum
│   ├── repository/          # Spring Data JPA repositories
│   │   ├── ForecastModelRepository.java    # ✅ Nowe repo
│   │   ├── ForecastRequestRepository.java  # ✅ Nowe repo
│   │   └── ForecastResultRepository.java   # ✅ Nowe repo
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── BloodInventoryService.java
│   │   ├── ScraperClientService.java
│   │   ├── ForecastService.java            # ✅ Nowy serwis
│   │   └── MLServiceClient.java            # ✅ Klient ML
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── BloodInventoryController.java
│   │   ├── AdminController.java
│   │   └── ForecastController.java         # ✅ Nowy kontroler
│   ├── security/            # JWT + Spring Security
│   ├── dto/                 # Data Transfer Objects
│   │   ├── ForecastRequestDto.java         # ✅ Nowe DTO
│   │   ├── ForecastResponseDto.java        # ✅ Nowe DTO
│   │   ├── ForecastResultDto.java          # ✅ Nowe DTO
│   │   ├── MLForecastRequest.java          # ✅ Nowe DTO
│   │   └── MLForecastResponse.java         # ✅ Nowe DTO
│   └── config/
│       ├── SecurityConfig.java
│       └── RestTemplateConfig.java         # ✅ Nowa konfiguracja
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
- **✅ `POST /api/forecast/create`** - Uruchom prognozę (ADMIN)
- **✅ `GET /api/forecast/{id}`** - Pobierz prognozę (ADMIN)
- **✅ `GET /api/forecast/all`** - Wszystkie prognozy (ADMIN)
- **✅ `GET /api/forecast/rckik/{rckikId}`** - Prognozy dla RCKiK (ADMIN)

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
│   │   │   ├── RCKiKScraperStrategy.java (interface)
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
- Spring Boot 3.4.1
- Java 21
- Gradle
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
│   │   └── Welcome.astro
│   ├── layouts/             # Layout templates
│   │   └── Layout.astro
│   ├── pages/               # Astro pages (routing)
│   │   └── index.astro
│   └── styles/              # CSS/Tailwind
│       └── global.css
├── public/                  # Static assets
├── package.json
├── astro.config.mjs
├── tailwind.config.mjs
└── Dockerfile
```

**Technologia:**
- Astro framework
- React (dla komponentów interaktywnych)
- Tailwind CSS
- TypeScript

**Port:** 4321

**Planowane funkcje:**
- Dashboard z mapą Polski i stanami RCKiK
- Wizualizacje szeregów czasowych
- Wyświetlanie prognoz ML
- System alertów o niedoborach
- Panel administratora

### 📂 `/ml` - Machine Learning ✅ ZAIMPLEMENTOWANY!
```
ml/
├── src/
│   ├── api/                 # ✅ Flask REST API
│   │   ├── __init__.py
│   │   └── app.py           # ✅ Główna aplikacja Flask
│   ├── models/              # ✅ Modele ML
│   │   ├── __init__.py
│   │   └── arima_forecaster.py  # ✅ Model ARIMA
│   ├── preprocessing/       # Przygotowanie danych
│   │   └── __init__.py
│   └── __init__.py
├── tests/                   # Testy jednostkowe
│   └── __init__.py
├── requirements.txt         # ✅ Zależności Python
├── Dockerfile              # ✅ Kontener Docker
├── .env.example            # ✅ Przykładowa konfiguracja
├── .gitignore
└── README.md               # ✅ Dokumentacja
```

**Technologia:**
- Python 3.11
- Flask 3.0.0 (REST API)
- statsmodels 0.14.1 (ARIMA, SARIMA)
- prophet 1.1.5 (Facebook Prophet)
- tensorflow 2.15.0 (LSTM)
- pandas, numpy (przetwarzanie danych)
- scikit-learn (ML utilities)
- gunicorn (production server)

**Port:** 5000

**✅ Zaimplementowane modele (WSZYSTKIE 4):**

1. **ARIMA(1,1,1)** - AutoRegressive Integrated Moving Average
   - Parametry: p=1, d=1, q=1
   - Minimum danych: 10 punktów
   - Horyzont: 1-7 dni (krótkoterminowy)
   - Najszybszy model

2. **PROPHET** - Facebook Prophet
   - Sezonowość: multiplicative
   - Minimum danych: 14 punktów
   - Horyzont: 1-30 dni (średnio/długoterminowy)
   - Uwzględnia święta i wzorce tygodniowe

3. **SARIMA(1,1,1)x(1,1,1,7)** - Seasonal ARIMA
   - Sezonowość: 7 dni (tygodniowa)
   - Minimum danych: 14 punktów
   - Horyzont: 1-14 dni
   - Wykrywa cykliczne wzorce

4. **LSTM** - Long Short-Term Memory (Deep Learning)
   - Lookback: 7 dni
   - LSTM units: 50
   - Epochs: 50
   - Minimum danych: 27 punktów
   - Najdokładniejszy dla złożonych wzorców

**API endpoints:**
- ✅ `GET /health` - Health check
- ✅ `POST /api/forecast` - Generowanie prognozy
  - Wspiera: ARIMA, PROPHET, SARIMA, LSTM
  - Input: historyczne dane + typ modelu + horyzont
  - Output: prognozy z interwałami ufności
- ✅ `GET /api/models` - Lista dostępnych modeli z parametrami

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
│  :4321   │               │  :8081   │
└──────────┘               └─────┬────┘
                                 │ REST
                                 ▼
                           ┌──────────┐
                           │    ML    │  ✅ DZIAŁA!
                           │  :5000   │
                           └──────────┘
```

### Szczegółowy flow prognozy:
1. **Admin** → Wywołuje prognozę przez `POST /api/forecast/create` (Backend)
2. **Backend** → Pobiera dane historyczne z `blood_inventory_record`
3. **Backend** → Przygotowuje request dla ML (`MLForecastRequest`)
4. **Backend** → Wysyła `POST http://ml:5000/api/forecast`
5. **ML Service** → Przetwarza dane, trenuje model ARIMA
6. **ML Service** → Generuje prognozę na N dni z przedziałami ufności
7. **ML Service** → Zwraca `MLForecastResponse`
8. **Backend** → Zapisuje wyniki do `forecast_result`
9. **Backend** → Zwraca `ForecastResponseDto` do klienta
10. **Frontend** → Wyświetla prognozy użytkownikowi

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
ml.service.url=http://localhost:5000              # ✅ Nowa konfiguracja
```

### Scraper (`application.properties`):
```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/mkrew
```

### ML Service (`.env`):
```env
PORT=5000
DEBUG=False
FLASK_ENV=production
DEFAULT_ARIMA_P=1
DEFAULT_ARIMA_D=1
DEFAULT_ARIMA_Q=1
CONFIDENCE_LEVEL=0.95
```

### Docker Compose:
```yaml
services:
  - postgres:5432        # Baza danych
  - scraper:8080         # Scraping service
  - ml:5000             # ✅ ML forecasting service
  - backend:8081        # REST API
  - frontend:4321       # Astro frontend
```

**Kolejność uruchamiania:**
1. postgres (healthcheck)
2. scraper + ml (równolegle)
3. backend (czeka na postgres + scraper + ml)
4. frontend (czeka na backend)

## 🚀 Uruchomienie projektu

### Wymagania:
- Java 21
- PostgreSQL 15+
- Gradle 8+
- Python 3.11+
- Node.js 18+
- Docker + Docker Compose (opcjonalnie)

### Uruchomienie lokalne:

#### 1. Baza danych:
```bash
# PostgreSQL musi działać na localhost:5432
# Utworzy się automatycznie przez migracje Liquibase
```

#### 2. ML Service:
```bash
cd ml
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
python src/api/app.py
# Działa na http://localhost:5000
```

#### 3. Scraper:
```bash
cd scraper
./gradlew bootRun
# Działa na http://localhost:8080
```

#### 4. Backend:
```bash
cd backend
./gradlew bootRun
# Działa na http://localhost:8081
```

#### 5. Frontend:
```bash
cd frontend
npm install
npm run dev
# Działa na http://localhost:4321
```

### Uruchomienie z Docker Compose:
```bash
# Wszystkie serwisy naraz
docker-compose up -d

# Build i uruchom
docker-compose up -d --build

# Sprawdź logi
docker-compose logs -f

# Zatrzymaj
docker-compose down
```

## 📊 Modele ML - Szczegóły

### ✅ ARIMA(1,1,1) - ZAIMPLEMENTOWANY
- **Zastosowanie:** Prognozowanie krótkoterminowe
- **Parametry:** p=1, d=1, q=1
- **Minimalna liczba danych:** 10 punktów
- **Horyzont:** 1-7 dni
- **Output:** Prognoza + przedziały ufności (95%)
- **Zalety:** Najszybszy, dobry dla stabilnych trendów

### ✅ Prophet - ZAIMPLEMENTOWANY
- **Zastosowanie:** Długoterminowe prognozy z sezonowością
- **Sezonowość:** Multiplicative (yearly, weekly)
- **Minimalna liczba danych:** 14 punktów
- **Horyzont:** 1-30 dni
- **Output:** Prognoza + przedziały ufności (95%)
- **Zalety:** Uwzględnia święta, weekendy, wzorce roczne

### ✅ SARIMA(1,1,1)x(1,1,1,7) - ZAIMPLEMENTOWANY
- **Zastosowanie:** Prognozy z wzorcami sezonowymi
- **Sezonowość:** 7 dni (tygodniowa)
- **Minimalna liczba danych:** 14 punktów
- **Horyzont:** 1-14 dni
- **Output:** Prognoza + przedziały ufności (95%)
- **Zalety:** Wykrywa cykliczne wzorce tygodniowe

### ✅ LSTM - ZAIMPLEMENTOWANY
- **Zastosowanie:** Złożone wzorce nieliniowe
- **Architektura:** 2-layer LSTM (50→25 units) + Dropout
- **Minimalna liczba danych:** 27 punktów (lookback=7 + 20 training)
- **Horyzont:** 1-7 dni
- **Output:** Prognoza + szacowane przedziały (95%)
- **Zalety:** Najdokładniejszy dla skomplikowanych wzorców

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
| Status | Numeric | Range |
|--------|---------|-------|
| CRITICALLY_LOW | 1 | < 1.5 |
| LOW | 2 | 1.5 - 2.5 |
| MEDIUM | 3 | 2.5 - 3.5 |
| SATISFACTORY | 4 | 3.5 - 4.5 |
| OPTIMAL | 5 | > 4.5 |

## 🔧 Technologie - Podsumowanie

| Komponent | Technologia | Port | Status | Cel |
|-----------|-------------|------|--------|-----|
| Database | PostgreSQL + Liquibase | 5432 | ✅ | Przechowywanie danych |
| Scraper | Spring Boot + Jsoup + Quartz | 8080 | ✅ | Web scraping RCKiK |
| **ML Service** | **Python + 4 modele ML** | **5000** | **✅** | **ARIMA, Prophet, SARIMA, LSTM** |
| Backend | Spring Boot 3.4 + Java 21 | 8081 | ✅ | REST API + logika biznesowa |
| Frontend | Astro + React + Tailwind | 4321 | 🔄 | UI/Dashboard |

## 📚 Dokumentacja dodatkowa

- `metodologia.md` - Pełna metodologia badawcza projektu
- `db/erd-diagram.drawio` - Diagram ERD bazy danych
- `backend/ARCHITECTURE.md` - Architektura backendu
- `backend/IMPLEMENTATION_GUIDE.md` - Przewodnik implementacji
- `ml/README.md` - ✅ Dokumentacja ML service

## ⚠️ Ważne uwagi dla AI/Asystentów

1. **Migracje bazy danych:** Używamy Liquibase (NIE Flyway!), pliki YAML w `/db/changelog/changes/`
2. **Struktura katalogów:** `/db` jest GŁÓWNYM miejscem dla bazy, NIE `backend/src/main/resources/db`
3. **✅ ML Service KOMPLETNY:** Moduł `/ml` zaimplementowany ze WSZYSTKIMI 4 modelami:
   - ✅ ARIMA - klasyczny model szeregów czasowych
   - ✅ Prophet - model z sezonowością
   - ✅ SARIMA - sezonowy ARIMA
   - ✅ LSTM - deep learning
4. **Testowe dane:** Admin user już utworzony w migracji 004
5. **API prognozy:**
   - Backend: ✅ Zaimplementowany (`ForecastController`, `ForecastService`, `MLServiceClient`)
   - ML Service: ✅ Zaimplementowany (`/api/forecast` wspiera 4 modele)
   - Frontend: 🔄 Do implementacji
6. **Docker:** Wszystkie serwisy mają Dockerfile i są w docker-compose.yml
7. **Komunikacja Backend-ML:**
   - Local: `http://localhost:5000`
   - Docker: `http://ml:5000`
8. **⚠️ NAPRAWIONO:** Typo `getConfideLevel()` → `getConfidenceLevel()` w MLForecastResponse.java

## 🎯 Kolejne kroki (TODO)

- [ ] Rozbudowa frontendu o wizualizacje prognoz
- [ ] System alertów i notyfikacji
- [ ] Dashboard administratora
- [ ] Metryki i monitoring dokładności prognoz
- [ ] Automatyczna optymalizacja parametrów modeli (auto-ARIMA)
- [ ] API do batch predictions (wiele prognoz naraz)
- [ ] Jupyter notebooks do eksperymentów i analiz
- [ ] Porównanie modeli (benchmark różnych algorytmów)
- [ ] Cache dla prognoz (Redis)
- [ ] Model persistence (zapisywanie wytrenowanych modeli)

---

**Ostatnia aktualizacja:** 2025-10-04 00:15
**Status projektu:**
- ✅ Backend - GOTOWY (z API prognoz, naprawiony typo)
- ✅ Scraper - GOTOWY
- ✅ ML Service - KOMPLETNY (4 modele: ARIMA, Prophet, SARIMA, LSTM)
- ✅ Database - GOTOWY (5 migracji)
- 🔄 Frontend - W TRAKCIE ROZWOJU

**Zmiany w ostatniej aktualizacji:**
1. ✅ Naprawiono typo `getConfideLevel()` → `getConfidenceLevel()` w MLForecastResponse.java
2. ✅ Dodano Prophet model (Facebook Prophet z sezonowością)
3. ✅ Dodano SARIMA model (sezonowy ARIMA, wzorce tygodniowe)
4. ✅ Dodano LSTM model (deep learning, TensorFlow)
5. ✅ Zaktualizowano ML API do obsługi wszystkich 4 modeli
6. ✅ Zaktualizowano requirements.txt (prophet, tensorflow)
7. ✅ Zaktualizowano dokumentację PROJECT_STRUCTURE.md
