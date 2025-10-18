# Dokument wymagań produktu (PRD) - mkrew2

## 1. Przegląd produktu

### 1.1. Nazwa produktu
mkrew2 - System Prognozowania i Wizualizacji Stanów Magazynowych Krwi

### 1.2. Wersja dokumentu
1.0 - MVP (Minimum Viable Product)

### 1.3. Kontekst akademicki
Projekt realizowany w ramach kursu 10xdevs 2.0 jako Proof of Concept (PoC)

### 1.4. Cel główny
Stworzenie systemu do automatycznego zbierania, prognozowania i wizualizacji stanów magazynowych krwi w 21 Regionalnych Centrach Krwiodawstwa i Krwiolecznictwa (RCKiK) w Polsce, wykorzystującego uczenie maszynowe do predykcji potencjalnych niedoborów krwi.

### 1.5. Zakres MVP
MVP koncentruje się wyłącznie na aspektach prognozowania i wizualizacji danych o stanach magazynowych krwi. System jest narzędziem administracyjnym i analitycznym dla zamkniętej grupy użytkowników (role: ADMIN, USER_DATA).

Platforma społecznościowa dla krwiodawstwa (deklaracje oddania krwi, prośby o krew, komunikacja użytkowników) jest planowana jako odrębna faza rozwoju (Faza 2).

### 1.6. Architektura systemu
System oparty na architekturze mikroserwisowej składającej się z 5 komponentów:

- Database (PostgreSQL + Liquibase) - port 5432
- Scraper (Spring Boot 3.4.1, Java 21) - port 8080
- ML Service (Python 3.11, Flask) - port 5000
- Backend (Spring Boot 3.4.1, Java 21, JWT) - port 8081
- Frontend (Astro 5.14, React 19, Tailwind CSS 4) - port 4321

### 1.7. Status implementacji
- Database: Kompletne (5 migracji Liquibase)
- Scraper: Kompletne (web scraping + harmonogramowanie)
- ML Service: Kompletne (4 modele: ARIMA, Prophet, SARIMA, LSTM)
- Backend: Kompletne (REST API + integracja z ML)
- Frontend: W rozwoju (struktura podstawowa, wymaga wizualizacji prognoz)

## 2. Problem użytkownika

### 2.1. Opis problemu
Regionalne Centra Krwiodawstwa i Krwiolecznictwa w Polsce borykają się z cyklicznymi niedoborami konkretnych grup krwi, które często występują w sposób przewidywalny (sezonowość, dni tygodnia, święta, wakacje). Brak skutecznego systemu wczesnego ostrzegania o nadchodzących niedoborach uniemożliwia proaktywne działania, takie jak:

- Planowanie akcji mobilnych krwiodawstwa w odpowiednim czasie
- Optymalizacja kampanii medialnych zachęcających do oddawania krwi
- Transfer zapasów krwi między centrami regionalnymi
- Efektywna alokacja zasobów RCKiK

### 2.2. Obecny stan
Obecnie dane o stanach magazynowych są publikowane na oficjalnych stronach internetowych 21 RCKiK w Polsce, ale:

- Brak jest centralnego systemu agregującego te dane
- Dane historyczne nie są archiwizowane w sposób systematyczny
- Nie istnieją narzędzia analityczne wykorzystujące uczenie maszynowe do prognozowania
- Decyzje podejmowane są reaktywnie (po wystąpieniu niedoboru), a nie proaktywnie

### 2.3. Grupy docelowe (MVP)
W fazie MVP system skierowany jest do:

1. Administratorzy systemu (rola ADMIN):
   - Zespół badawczy realizujący projekt akademicki
   - Potencjalni koordynatorzy z Narodowego Centrum Krwi (NCK)

2. Użytkownicy danych (rola USER_DATA):
   - Analitycy zainteresowani danymi o stanach magazynowych krwi
   - Potencjalni przedstawiciele RCKiK (po zainteresowaniu współpracą)

Uwaga: Dawcy krwi i osoby potrzebujące transfuzji będą grupami docelowymi w Fazie 2 (platforma społecznościowa).

### 2.4. Zakres danych
- 21 Regionalnych Centrów Krwiodawstwa w Polsce: Białystok, Bydgoszcz, Gdańsk, Kalisz, Katowice, Kielce, Kraków, Lublin, Łódź, Olsztyn, Opole, Poznań, Racibórz, Radom, Rzeszów, Słupsk, Szczecin, Wałbrzych, Warszawa, Wrocław, Zielona Góra
- 8 grup krwi: A+, A-, B+, B-, AB+, AB-, O+, O-
- 5 kategorii stanu magazynowego (w dniach zapasów):
  - CRITICALLY_LOW (1): < 1.5 dni zapasów
  - LOW (2): 1.5 - 2.5 dni zapasów
  - MEDIUM (3): 2.5 - 3.5 dni zapasów
  - SATISFACTORY (4): 3.5 - 4.5 dni zapasów
  - OPTIMAL (5): > 4.5 dni zapasów

## 3. Wymagania funkcjonalne

### 3.1. Zbieranie danych (Scraper Service)

#### 3.1.1. Automatyczne pobieranie danych
- System automatycznie zbiera dane o stanach magazynowych z 21 oficjalnych stron RCKiK
- Harmonogram: 3 razy dziennie (8:00, 14:00, 20:00) - Quartz Scheduler
- Technologia: Jsoup (web scraping), strategia dla każdego RCKiK (Strategy Pattern)
- Zabezpieczenia: API Key authentication, network isolation w Docker

#### 3.1.2. Logowanie operacji
- Każda operacja scrapingu jest logowana w tabeli scraping_log
- Dane logów: timestamp, RCKiK, status (SUCCESS/FAILURE), liczba pobranych rekordów, komunikaty błędów

#### 3.1.3. Trigger ręczny (ADMIN)
Administrator może wywołać scraping manualnie dla wszystkich RCKiK lub wybranego centrum.

Architektura endpointów (dwupoziomowa):

Backend API (port 8081) - wywoływane przez użytkownika:
- POST /api/admin/scraper/trigger-all - Proxy do Scraper Service (wymaga roli ADMIN + JWT)
- POST /api/admin/scraper/trigger/{rckikCode} - Proxy do Scraper Service (wymaga roli ADMIN + JWT)

Scraper Service (port 8080) - wywoływane przez Backend:
- POST /api/scraper/trigger-all - Faktyczny endpoint scrapingu (wymaga API Key)
- POST /api/scraper/trigger/{rckikCode} - Scraping pojedynczego RCKiK (wymaga API Key)
- GET /api/scraper/health - Health check (publiczny, bez API Key)

Uwaga: Backend automatycznie dołącza API Key do requestów wysyłanych do Scraper Service.

### 3.2. Modele uczenia maszynowego (ML Service)

#### 3.2.1. Dostępne modele
System implementuje 4 modele predykcyjne:

1. ARIMA(1,1,1) - AutoRegressive Integrated Moving Average
   - Zastosowanie: Prognozy krótkoterminowe (1-7 dni)
   - Minimalne dane: 10 punktów
   - Najszybszy model

2. Prophet (Facebook Prophet)
   - Zastosowanie: Prognozy średnioterminowe (1-30 dni)
   - Minimalne dane: 14 punktów
   - Obsługuje sezonowość, święta, wzorce tygodniowe

3. SARIMA(1,1,1)x(1,1,1,7) - Seasonal ARIMA
   - Zastosowanie: Prognozy z sezonowością tygodniową (1-14 dni)
   - Minimalne dane: 14 punktów
   - Wykrywa wzorce cykliczne

4. LSTM - Long Short-Term Memory (Deep Learning)
   - Zastosowanie: Złożone wzorce nieliniowe (1-7 dni)
   - Minimalne dane: 27 punktów (7 lookback + 20 training)
   - Architektura: 2-warstwowy LSTM (50→25 units) + Dropout (0.2)
   - Parametry treningu:
     - Lookback window: 7 dni
     - Epochs: 50
     - Batch size: 32
     - Optimizer: Adam
   - Najdokładniejszy dla skomplikowanych wzorców

#### 3.2.2. API ML Service
- Endpoint: POST /api/forecast (wymaga API key)
- Endpoint: GET /api/models (lista dostępnych modeli z parametrami)
- Endpoint: GET /health (publiczny, bez API key)
- Zabezpieczenia: API Key authentication (@require_api_key decorator), network isolation w Docker

#### 3.2.3. Format odpowiedzi ML
- Prognozy z 95% przedziałami ufności (confidence intervals)
- Zwracane wartości: przewidywany stan, dolna granica, górna granica, timestamp
- Metadane: model użyty, parametry, wersja

### 3.3. Backend API

#### 3.3.1. Uwierzytelnianie i autoryzacja
- Metoda: JWT (Bearer tokens) z Spring Security
- Role:
  - ADMIN: Pełny dostęp (scraping, prognozy, zarządzanie użytkownikami)
  - USER_DATA: Dostęp read-only do danych i prognoz
- Domyślni użytkownicy:
  - Admin: username=admin, password=admin123
  - User: username=user, password=user123

#### 3.3.2. Endpointy autoryzacyjne
- POST /api/auth/login - Logowanie (zwraca JWT token)

#### 3.3.3. Endpointy danych o krwi (USER_DATA, ADMIN)
- GET /api/blood-inventory/current - Obecny stan dla wszystkich RCKiK
- GET /api/blood-inventory/current/{rckikCode} - Stan dla konkretnego RCKiK
- GET /api/blood-inventory/history/{rckikCode}?bloodType={type}&period={days} - Historia (1, 7, 30, 90, 365 dni)

#### 3.3.4. Endpointy administracyjne (ADMIN)
- POST /api/admin/scraper/trigger-all - Wywołanie scrapingu dla wszystkich RCKiK
- POST /api/admin/scraper/trigger/{rckikCode} - Scraping dla konkretnego centrum

#### 3.3.5. Endpointy prognoz

Tworzenie prognoz (wymagana rola ADMIN):
- POST /api/forecast/create - Generowanie nowej prognozy

Odczyt prognoz (role: ADMIN, USER_DATA):
- GET /api/forecast/{id} - Pobranie prognozy po ID
- GET /api/forecast/all - Lista wszystkich prognoz
- GET /api/forecast/rckik/{rckikId} - Prognozy dla konkretnego RCKiK

Uwaga: USER_DATA ma dostęp read-only do prognoz (może przeglądać, ale nie tworzyć).

### 3.4. Frontend Dashboard

#### 3.4.1. Strona główna / Dashboard (ADMIN, USER_DATA)
Pojedyncza strona z dynamicznym filtrowaniem składająca się z:

Górna sekcja - Tabela/Grid statusów:
- Przegląd wszystkich 21 RCKiK
- Wyświetlanie aktualnego stanu dla wszystkich 8 grup krwi
- Kolorowe kodowanie stanów:
  - Czerwony: CRITICALLY_LOW, LOW
  - Żółty: MEDIUM
  - Zielony: SATISFACTORY, OPTIMAL
- Możliwość sortowania i filtrowania

Filtry i kontrolki:
- Dropdown wyboru RCKiK (domyślnie: wszystkie)
- Dropdown wyboru grupy krwi (A+, A-, B+, B-, AB+, AB-, O+, O-, wszystkie)
- Selektor okresu historycznego (30 dni, 90 dni)
- Selektor horyzontu prognozy (7 dni, 14 dni)

Dolna sekcja - Wizualizacje (dynamiczne, aktualizowane po zmianie filtrów):
- Wykres liniowy historii stanów magazynowych (ostatnie 30/90 dni)
- Wykres prognozy z przedziałami ufności (7/14 dni) - linia środkowa + obszar ufności
- Legenda z opisem poziomów stanów

Panel boczny/Sekcja alertów:
- Lista alertów informacyjnych, ostrzeżeń i alertów krytycznych
- Format: "RCKiK Kraków: przewidywany niedobór AB+ za 5 dni (pewność 78%)"
- Poziomy alertów w MVP (zgodnie z metodologią badawczą):
  - Poziom 1 - Informacyjny (niebieski/szary): Przewidywany spadek do stanu średniego (MEDIUM) w ciągu 7 dni
  - Poziom 2 - Ostrzeżenie (pomarańczowy): Przewidywany spadek do stanu niskiego (LOW) w ciągu 7 dni
  - Poziom 3 - Krytyczny (czerwony): Przewidywany spadek do stanu niskiego (LOW lub CRITICALLY_LOW) w ciągu 3 dni LUB stan bardzo niski (CRITICALLY_LOW) przewidywany przez >7 dni
- Kolorowe oznaczenia priorytetów

#### 3.4.2. Panel Admin (tylko ADMIN)
Osobny widok z funkcjami administracyjnymi:

Sekcja Scraping:
- Przycisk "Trigger scraping - wszystkie RCKiK"
- Selektor + przycisk dla pojedynczego RCKiK
- Tabela ostatnich operacji scrapingu (timestamp, RCKiK, status, liczba rekordów)

Sekcja Prognozy:
- Formularz generowania prognozy:
  - Wybór RCKiK (pojedynczy lub "wszystkie")
  - Wybór grupy krwi (pojedyncza lub "wszystkie")
  - Wybór modelu ML (ARIMA, Prophet, SARIMA, LSTM)
  - Horyzont prognozy (7, 14, 30 dni)
  - Przycisk "Generuj prognozę"
- Opcja "Batch forecast": jeden przycisk generujący prognozy dla wszystkich 21 RCKiK i wszystkich grup krwi jednocześnie
- Tabela wygenerowanych prognoz (timestamp, RCKiK, grupa krwi, model, status)

Sekcja Logi systemowe:
- Historia operacji scrapingu
- Logi błędów
- Filtrowanie po dacie i typie operacji

Sekcja Zarządzanie użytkownikami (opcjonalnie w MVP):
- Lista użytkowników
- Dodawanie nowego użytkownika USER_DATA (formularz: username, email, password, rola)
- Usuwanie użytkownika
- Uwaga: W PoC można pominąć i mieć stałych 2-3 użytkowników testowych

#### 3.4.3. Eksport danych
- Przycisk "Eksport do CSV" dla danych historycznych
- Przycisk "Eksport prognoz do CSV"
- Format: CSV z kolumnami: data, RCKiK, grupa_krwi, stan, [dla prognoz: przewidywany_stan, dolna_granica, górna_granica, model]

### 3.5. Walidacja i monitoring dokładności prognoz

#### 3.5.1. Archiwizacja prognoz
- Wszystkie wygenerowane prognozy zapisywane w tabeli forecast_result
- Dane: timestamp utworzenia, RCKiK, grupa krwi, model, horyzont, przewidywane wartości z przedziałami ufności

#### 3.5.2. Porównanie prognozy vs rzeczywistość
- Dashboard pokazuje zestawienie: "Co przewidywaliśmy 7 dni temu" vs "Co się faktycznie stało"
- Wizualizacja: nakładanie się wykresu prognozy i rzeczywistych wartości

#### 3.5.3. Metryki dokładności
System oblicza i wyświetla następujące metryki dla każdego modelu:
- MAE (Mean Absolute Error)
- RMSE (Root Mean Squared Error)
- Accuracy (dokładność klasyfikacji stanów)
- Precision, Recall, F1-score (szczególnie dla klasy "niski stan")

#### 3.5.4. Raportowanie
- Sekcja "Ewaluacja modeli" w panelu admin
- Tabela porównawcza dokładności modeli
- Możliwość filtrowania po RCKiK, grupie krwi, okresie

### 3.6. Bezpieczeństwo

System implementuje wielopoziomową architekturę bezpieczeństwa.

#### 3.6.1. Backend - Uwierzytelnianie użytkowników
- Metoda: JWT (JSON Web Tokens) + Spring Security
- Role: ADMIN (pełny dostęp), USER_DATA (read-only)
- Hasła: BCrypt hashing (salt rounds: 10)
- JWT Secret: konfiguracja w application.properties (256-bit key, ZMIENIĆ W PRODUKCJI)
- Token lifetime: 24 godziny (konfigurowalne)

#### 3.6.2. Scraper Service - Dwupoziomowa ochrona

Poziom 1 - API Key Authentication (Warstwa aplikacyjna):
- Wszystkie endpointy (oprócz /health) wymagają nagłówka X-API-Key
- Implementacja: ApiKeyFilter.java waliduje klucz przed wykonaniem requesta
- Konfiguracja: property scraper.api.key (zmienna środowiskowa: SCRAPER_API_KEY)
- Domyślny klucz: change-this-secure-api-key-in-production-mkrew-scraper-2024
- ⚠️ ZMIENIĆ W PRODUKCJI!
- Backend automatycznie dołącza API Key do wszystkich requestów do Scraper

Poziom 2 - Network Isolation (Warstwa infrastrukturalna - tylko Docker):
- Port 8080 NIE jest wystawiony na host (docker-compose używa expose zamiast ports)
- Scraper dostępny tylko w sieci Docker mkrew-network
- Backend komunikuje się przez wewnętrzne DNS: http://scraper:8080
- Zewnętrzne requesty z internetu nie mogą dotrzeć bezpośrednio do Scraper
- Dodatkowa warstwa obrony nawet jeśli API Key wycieknie

W środowisku lokalnym (bez Docker):
- Scraper dostępny pod http://localhost:8080 (port otwarty dla developmentu)
- Nadal wymagany API Key dla chronionych endpointów

#### 3.6.3. ML Service - Dwupoziomowa ochrona

Poziom 1 - API Key Authentication (Warstwa aplikacyjna):
- Wszystkie endpointy (oprócz /health) wymagają nagłówka X-API-Key
- Implementacja: auth_middleware.py z dekoratorem @require_api_key
- Konfiguracja: zmienna środowiskowa ML_API_KEY
- Domyślny klucz: change-this-secure-api-key-in-production-mkrew-ml-2024
- ⚠️ ZMIENIĆ W PRODUKCJI!
- Backend automatycznie dołącza API Key do wszystkich requestów do ML Service

Poziom 2 - Network Isolation (Warstwa infrastrukturalna - tylko Docker):
- Port 5000 NIE jest wystawiony na host (docker-compose używa expose zamiast ports)
- ML Service dostępny tylko w sieci Docker mkrew-network
- Backend komunikuje się przez wewnętrzne DNS: http://ml:5000
- Zewnętrzne requesty z internetu nie mogą dotrzeć bezpośrednio do ML Service
- Dodatkowa warstwa obrony nawet jeśli API Key wycieknie

W środowisku lokalnym (bez Docker):
- ML Service dostępny pod http://localhost:5000 (port otwarty dla developmentu)
- Nadal wymagany API Key dla chronionych endpointów

#### 3.6.4. Konfiguracja bezpieczeństwa

Zmienne środowiskowe (wymagane w produkcji):
- SCRAPER_API_KEY - klucz API dla Scraper Service
- ML_API_KEY - klucz API dla ML Service
- JWT_SECRET - secret dla tokenów JWT (Backend)

Pliki konfiguracyjne:
- .env.example - Przykładowe zmienne środowiskowe (root projektu)
- backend/src/main/resources/application.properties - Backend config
- scraper/src/main/resources/application.properties - Scraper config
- ml/.env.example - ML Service config

⚠️ Bezpieczeństwo produkcyjne:
1. ZMIENIĆ wszystkie domyślne klucze API przed wdrożeniem
2. ZMIENIĆ JWT_SECRET na losowy 256-bit key
3. Używać silnych haseł dla użytkowników
4. W Docker NIGDY nie wystawiać portów Scraper i ML na host (tylko expose)
5. Rotacja kluczy API co 90 dni (best practice)

#### 3.6.5. Ochrona danych
- Dane medyczne: system nie przechowuje danych osobowych pacjentów
- Dane publiczne: stany magazynowe są publicznie dostępne na stronach RCKiK
- RODO: system nie wymaga zgody użytkowników na przetwarzanie danych (dane publiczne)
- HTTPS: planowane w środowisku produkcyjnym (poza zakresem MVP)
- Backup: automatyczne backup PostgreSQL (docker volume postgres_data)

## 4. Granice produktu

### 4.1. Co WCHODZI w zakres MVP

#### 4.1.1. Funkcjonalności podstawowe
- Automatyczne zbieranie danych z 21 oficjalnych stron RCKiK (3x dziennie)
- Ręczne wywoływanie scrapingu przez ADMIN
- Archiwizacja historycznych stanów magazynowych w bazie PostgreSQL
- Generowanie prognoz krótko- i średnioterminowych (1-7 dni, 1-4 tygodnie)
- 4 modele ML: ARIMA, Prophet, SARIMA, LSTM
- Prognozy z 95% przedziałami ufności
- Dashboard z wizualizacjami (wykresy historyczne, prognozy)
- System alertów (wizualny + lista ostrzeżeń)
- Walidacja dokładności prognoz (porównanie z rzeczywistością)
- Metryki ewaluacji modeli (MAE, RMSE, Accuracy, Precision, Recall, F1)
- Eksport danych i prognoz do CSV
- Role użytkowników: ADMIN (pełny dostęp), USER_DATA (read-only)
- Uwierzytelnianie JWT
- API Key security dla Scraper i ML Service

#### 4.1.2. Użytkownicy MVP
- Administratorzy systemu (zespół badawczy)
- Użytkownicy z dostępem do danych (analitycy, potencjalni przedstawiciele NCK/RCKiK)
- Zamknięty system - brak publicznego dostępu

#### 4.1.3. Technologie
- Database: PostgreSQL 16 + Liquibase (YAML migrations)
- Backend: Spring Boot 3.4.1, Java 21, Spring Security, JWT
- Scraper: Spring Boot 3.4.1, Java 21, Jsoup, Quartz Scheduler
- ML Service: Python 3.11, Flask, scikit-learn, Prophet, TensorFlow/Keras
- Frontend: Astro 5.14, React 19, Tailwind CSS 4
- Deployment: Docker Compose, Docker network isolation

#### 4.1.4. Dane
- 8 grup krwi (pełna krew): A+, A-, B+, B-, AB+, AB-, O+, O-
- 21 RCKiK w Polsce
- Dane z oficjalnych stron internetowych (web scraping)

### 4.2. Co NIE WCHODZI w zakres MVP (Faza 2 i dalsze)

#### 4.2.1. Platforma społecznościowa
- Rejestracja użytkowników końcowych (dawcy krwi, osoby potrzebujące transfuzji)
- Publiczny dostęp do dashboardu
- Deklaracje oddania krwi przez użytkowników
- Prośby o krew dla konkretnych osób
- Komunikacja między użytkownikami (wbudowany komunikator)
- Wymiana danych kontaktowych
- Profil użytkownika z historią donacji
- Gamifikacja (odznaki, rankingi, liczniki oddań krwi)

#### 4.2.2. Powiadomienia aktywne
- Email notifications o przewidywanych niedoborach
- SMS notifications
- Push notifications (aplikacja mobilna)
- Personalizowane alerty dla dawców ("potrzebna Twoja grupa krwi")

#### 4.2.3. Zaawansowane funkcjonalności
- Automatyzacja generowania prognoz (scheduled jobs dla prognoz)
- System rekomendacji akcji (sugestie planowania akcji mobilnych)
- Optymalizacja logistyki transferu krwi między centrami
- Integracja API z systemami RCKiK/NCK
- Prognozy dla składników krwi (osocze, płytki, krwinki czerwone)
- Auto-ARIMA (automatyczna optymalizacja parametrów modeli)
- Model persistence (zapisywanie wytrenowanych modeli)
- Redis caching dla prognoz
- Batch prediction API

#### 4.2.4. Weryfikacje i compliance
- Weryfikacja tożsamości użytkowników (PESEL, dowód osobisty)
- Potwierdzenia medyczne dla próśb o krew
- Certyfikacja medyczna systemu
- Formalna współpraca z NCK/RCKiK
- Integracja z systemami szpitalnymi

#### 4.2.5. Raportowanie i analytics
- Raporty PDF
- Zaawansowane dashboardy analityczne
- BI (Business Intelligence) tools integration
- Jupyter notebooks dla eksperymentów
- A/B testing modeli

#### 4.2.6. Infrastruktura produkcyjna
- Deployment na GCP (częściowo przygotowany):
  - ✅ Terraform configuration for Cloud SQL PostgreSQL (zaimplementowane)
  - ✅ Cloud Build pipeline for database migrations (zaimplementowane)
  - ✅ Deployment scripts for ML service and backend (zaimplementowane)
  - ⏳ Pełne wdrożenie i testy - poza zakresem MVP
- HTTPS/SSL certificates (Cloud Load Balancer)
- Monitoring i logging produkcyjny (Cloud Logging, Cloud Monitoring, Prometheus, Grafana)
- Backup i disaster recovery (Cloud SQL automated backups)
- Load balancing (Cloud Load Balancer)
- Auto-scaling (Cloud Run lub GKE)

Uwaga: Konfiguracja GCP istnieje w repozytorium jako przygotowanie do przyszłego wdrożenia produkcyjnego, ale w MVP system działa wyłącznie lokalnie (Docker Compose).

## 5. Historyjki użytkowników

### 5.1. Uwierzytelnianie i autoryzacja

#### US-001: Logowanie do systemu
- ID: US-001
- Tytuł: Logowanie do systemu jako administrator lub użytkownik danych
- Opis: Jako użytkownik (ADMIN lub USER_DATA) chcę zalogować się do systemu używając nazwy użytkownika i hasła, aby uzyskać dostęp do funkcjonalności zgodnych z moją rolą
- Kryteria akceptacji:
  - Formularz logowania zawiera pola: username, password
  - System waliduje dane logowania z bazą danych
  - Po poprawnym logowaniu zwracany jest JWT token
  - Token jest zapisywany po stronie klienta (localStorage/sessionStorage)
  - Nieprawidłowe dane logowania skutkują komunikatem błędu "Nieprawidłowa nazwa użytkownika lub hasło"
  - Token jest dołączany do wszystkich kolejnych requestów (header: Authorization: Bearer {token})
  - Token ma określony czas ważności (np. 24h)

#### US-002: Przekierowanie do dashboardu po zalogowaniu
- ID: US-002
- Tytuł: Przekierowanie do odpowiedniego widoku po zalogowaniu
- Opis: Jako zalogowany użytkownik chcę zostać przekierowany do odpowiedniego widoku zgodnego z moją rolą, aby od razu rozpocząć pracę
- Kryteria akceptacji:
  - ADMIN po zalogowaniu widzi Dashboard główny z pełnym dostępem + link do Panelu Admin
  - USER_DATA po zalogowaniu widzi Dashboard główny z dostępem read-only (bez Panelu Admin)
  - Nagłówek strony wyświetla nazwę zalogowanego użytkownika i jego rolę
  - Dostępny przycisk "Wyloguj"

#### US-003: Wylogowanie z systemu
- ID: US-003
- Tytuł: Wylogowanie z systemu
- Opis: Jako zalogowany użytkownik chcę móc się wylogować z systemu, aby zakończyć sesję
- Kryteria akceptacji:
  - Przycisk "Wyloguj" dostępny w nagłówku
  - Po kliknięciu token JWT jest usuwany z localStorage
  - Użytkownik przekierowany do strony logowania
  - Próba dostępu do chronionych endpointów po wylogowaniu skutkuje błędem 401 Unauthorized

#### US-004: Odmowa dostępu dla nieautoryzowanych użytkowników
- ID: US-004
- Tytuł: Blokada dostępu do funkcjonalności bez autoryzacji
- Opis: Jako system chcę blokować dostęp do chronionych zasobów dla użytkowników niezalogowanych lub bez odpowiedniej roli
- Kryteria akceptacji:
  - Request bez tokenu JWT do chronionego endpointu zwraca 401 Unauthorized
  - Request z nieważnym tokenem zwraca 401 Unauthorized
  - Request z ważnym tokenem ale niewystarczającą rolą zwraca 403 Forbidden
  - USER_DATA próbujący wywołać endpoint ADMIN-only (np. POST /api/admin/scraper/trigger-all) otrzymuje błąd 403
  - Frontend ukrywa elementy UI niedostępne dla danej roli (np. Panel Admin dla USER_DATA)

### 5.2. Przeglądanie danych o stanach magazynowych

#### US-005: Wyświetlenie obecnego stanu wszystkich RCKiK
- ID: US-005
- Tytuł: Przegląd obecnego stanu magazynowego we wszystkich RCKiK
- Opis: Jako zalogowany użytkownik (ADMIN lub USER_DATA) chcę widzieć obecny stan magazynowy krwi dla wszystkich 21 RCKiK w formie tabeli/gridu, aby szybko ocenić sytuację w całym kraju
- Kryteria akceptacji:
  - Tabela zawiera 21 wierszy (każdy RCKiK)
  - Kolumny: Nazwa RCKiK, A+, A-, B+, B-, AB+, AB-, O+, O-
  - Każda komórka kolorowo zakodowana według stanu:
    - Czerwony: CRITICALLY_LOW, LOW
    - Żółty: MEDIUM
    - Zielony: SATISFACTORY, OPTIMAL
  - Legenda kolorów widoczna nad/pod tabelą
  - Możliwość sortowania po nazwie RCKiK (alfabetycznie)
  - Możliwość filtrowania: pokazanie tylko RCKiK ze stanami krytycznymi/niskimi
  - Dane aktualizowane z endpointu GET /api/blood-inventory/current

#### US-006: Filtrowanie danych po RCKiK
- ID: US-006
- Tytuł: Filtrowanie danych dla wybranego RCKiK
- Opis: Jako zalogowany użytkownik chcę wybrać konkretne RCKiK z listy, aby zobaczyć szczegółowe dane tylko dla tego centrum
- Kryteria akceptacji:
  - Dropdown "Wybierz RCKiK" z opcjami: "Wszystkie", "Białystok", "Bydgoszcz", ... (21 RCKiK)
  - Po wyborze konkretnego RCKiK:
    - Tabela w górnej sekcji pokazuje tylko ten RCKiK
    - Wykresy w dolnej sekcji aktualizują się dla wybranego centrum
  - Wybór "Wszystkie" przywraca widok pełnej tabeli
  - Zmiana wyboru natychmiast aktualizuje dane (bez przeładowania strony)

#### US-007: Filtrowanie danych po grupie krwi
- ID: US-007
- Tytuł: Filtrowanie danych dla wybranej grupy krwi
- Opis: Jako zalogowany użytkownik chcę wybrać konkretną grupę krwi, aby zobaczyć dane tylko dla tej grupy we wszystkich RCKiK
- Kryteria akceptacji:
  - Dropdown "Wybierz grupę krwi" z opcjami: "Wszystkie", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
  - Po wyborze konkretnej grupy krwi:
    - Tabela w górnej sekcji podświetla/filtruje tylko tę kolumnę
    - Wykresy w dolnej sekcji pokazują dane dla tej grupy
  - Wybór "Wszystkie" przywraca widok wszystkich grup
  - Możliwość kombinacji filtrów (RCKiK + grupa krwi)

#### US-008: Wyświetlenie historii stanów magazynowych
- ID: US-008
- Tytuł: Przeglądanie historycznych danych stanów magazynowych
- Opis: Jako zalogowany użytkownik chcę widzieć wykres historii stanów magazynowych dla wybranego RCKiK i grupy krwi, aby zrozumieć trendy i wzorce
- Kryteria akceptacji:
  - Wykres liniowy w dolnej sekcji dashboardu
  - Oś X: data (ostatnie 30 lub 90 dni, wybór przez selektor)
  - Oś Y: stan magazynowy (1-5: CRITICALLY_LOW do OPTIMAL)
  - Linia wykresu kolorowana zgodnie z poziomami stanów
  - Tooltip po najechaniu myszą: data, wartość stanu, kategoria tekstowa
  - Dane pobierane z GET /api/blood-inventory/history/{rckikCode}?bloodType={type}&period={days}
  - Selektor okresu: "30 dni", "90 dni"
  - Wykres aktualizuje się dynamicznie po zmianie filtrów (RCKiK, grupa krwi, okres)

#### US-009: Eksport danych historycznych do CSV
- ID: US-009
- Tytuł: Eksport danych historycznych do pliku CSV
- Opis: Jako zalogowany użytkownik chcę wyeksportować dane historyczne do pliku CSV, aby przeprowadzić własne analizy w Excel lub innych narzędziach
- Kryteria akceptacji:
  - Przycisk "Eksport do CSV" widoczny w sekcji danych historycznych
  - Eksport uwzględnia aktywne filtry (RCKiK, grupa krwi, okres)
  - Format CSV: data, RCKiK, grupa_krwi, stan_kategoria, stan_numeryczny
  - Nagłówki kolumn w pierwszym wierszu
  - Nazwa pliku: mkrew_historia_{RCKiK}_{grupa}_{data}.csv (np. mkrew_historia_Krakow_AB+_2025-01-15.csv)
  - Plik automatycznie pobierany przez przeglądarkę

### 5.3. Prognozy (ADMIN)

#### US-010: Generowanie prognozy dla pojedynczego RCKiK i grupy krwi
- ID: US-010
- Tytuł: Generowanie prognozy dla wybranego RCKiK, grupy krwi i modelu ML
- Opis: Jako ADMIN chcę wygenerować prognozę dla konkretnego RCKiK, grupy krwi, używając wybranego modelu ML i horyzontu czasowego, aby przewidzieć przyszłe stany magazynowe
- Kryteria akceptacji:
  - Formularz w Panelu Admin zawiera pola:
    - Dropdown "RCKiK": lista 21 centrów
    - Dropdown "Grupa krwi": A+, A-, B+, B-, AB+, AB-, O+, O-
    - Dropdown "Model ML": ARIMA, Prophet, SARIMA, LSTM
    - Dropdown "Horyzont prognozy": 7 dni, 14 dni, 30 dni
  - Przycisk "Generuj prognozę"
  - Po kliknięciu:
    - Request POST /api/forecast/create z parametrami
    - Wyświetlenie loadera/spinnera "Generowanie prognozy..."
    - Po zakończeniu: komunikat sukcesu "Prognoza wygenerowana pomyślnie" lub błąd
  - Wygenerowana prognoza zapisana w bazie (tabela forecast_result)
  - Prognoza dostępna do przeglądania w dashboardzie głównym

#### US-011: Generowanie prognoz wsadowych (batch forecast)
- ID: US-011
- Tytuł: Generowanie prognoz dla wszystkich RCKiK i grup krwi jednocześnie
- Opis: Jako ADMIN chcę wygenerować prognozy dla wszystkich 21 RCKiK i wszystkich 8 grup krwi jednym kliknięciem, aby zaoszczędzić czas
- Kryteria akceptacji:
  - Przycisk "Batch Forecast - wszystkie RCKiK i grupy" w Panelu Admin
  - Dialog potwierdzenia: "Czy na pewno chcesz wygenerować prognozy dla wszystkich 168 kombinacji (21 RCKiK x 8 grup)?"
  - Pola do wyboru:
    - Model ML (wspólny dla wszystkich): ARIMA, Prophet, SARIMA, LSTM
    - Horyzont: 7 dni, 14 dni, 30 dni
  - Po potwierdzeniu:
    - Uruchomienie procesu generowania (asynchronicznie)
    - Progress bar: "Wygenerowano X/168 prognoz"
  - Po zakończeniu: podsumowanie "168 prognoz wygenerowanych, 165 sukces, 3 błędy"
  - Lista błędów (jeśli wystąpiły) z informacją: RCKiK, grupa krwi, przyczyna błędu

#### US-012: Przeglądanie listy wygenerowanych prognoz
- ID: US-012
- Tytuł: Przegląd listy wszystkich wygenerowanych prognoz
- Opis: Jako ADMIN chcę widzieć listę wszystkich wygenerowanych prognoz z możliwością filtrowania i sortowania
- Kryteria akceptacji:
  - Tabela w Panelu Admin sekcji "Prognozy"
  - Kolumny: ID, Data utworzenia, RCKiK, Grupa krwi, Model, Horyzont, Status
  - Możliwość sortowania po dacie (najnowsze pierwsze)
  - Filtrowanie po RCKiK, grupie krwi, modelu
  - Przycisk "Szczegóły" w każdym wierszu → przekierowanie do wizualizacji prognozy
  - Dane z endpointu GET /api/forecast/all

#### US-013: Wizualizacja prognozy z przedziałami ufności
- ID: US-013
- Tytuł: Wyświetlenie wykresu prognozy z przedziałami ufności
- Opis: Jako zalogowany użytkownik (ADMIN lub USER_DATA) chcę widzieć wykres prognozy z przedziałami ufności, aby ocenić przyszłe stany magazynowe i niepewność predykcji
- Kryteria akceptacji:
  - Wykres liniowy w dolnej sekcji dashboardu (obok wykresu historycznego)
  - Oś X: data (przyszłe dni: +1 do +7/+14/+30)
  - Oś Y: stan magazynowy (1-5)
  - Trzy elementy wykresu:
    - Linia środkowa: przewidywany stan
    - Obszar zacieniony: przedział ufności 95% (dolna i górna granica)
    - Kolory zgodne z poziomami stanów
  - Tooltip: data, przewidywany stan, dolna granica, górna granica, pewność %
  - Legenda: "Prognoza", "Przedział ufności 95%"
  - Informacja: "Model: ARIMA, wygenerowano: 2025-01-15 10:30"

#### US-014: Porównanie prognozy z rzeczywistością
- ID: US-014
- Tytuł: Porównanie przewidywanych wartości z rzeczywistymi danymi
- Opis: Jako ADMIN chcę porównać prognozy wygenerowane wcześniej z rzeczywistymi danymi zebranymi później, aby ocenić dokładność modeli
- Kryteria akceptacji:
  - Sekcja "Walidacja prognoz" w Panelu Admin
  - Wybór prognozy do walidacji (lista prognoz starszych niż horyzont prognozy)
  - Wykres nakładający się:
    - Linia niebieska: przewidywane wartości z prognozy
    - Linia czerwona: rzeczywiste wartości ze scrapingu
  - Metryki dokładności wyświetlone obok:
    - MAE (Mean Absolute Error)
    - RMSE (Root Mean Squared Error)
    - Accuracy (% poprawnie przewidzianych kategorii stanów)
  - Możliwość eksportu porównania do CSV

#### US-015: Przegląd metryk ewaluacji modeli
- ID: US-015
- Tytuł: Wyświetlenie metryk dokładności dla różnych modeli ML
- Opis: Jako ADMIN chcę porównać dokładność różnych modeli ML (ARIMA, Prophet, SARIMA, LSTM), aby wybrać najlepszy model do prognozowania
- Kryteria akceptacji:
  - Sekcja "Ewaluacja modeli" w Panelu Admin
  - Tabela porównawcza:
    - Wiersze: modele (ARIMA, Prophet, SARIMA, LSTM)
    - Kolumny: MAE, RMSE, Accuracy, Precision, Recall, F1-score
  - Możliwość filtrowania po RCKiK, grupie krwi, okresie
  - Kolorowe oznaczenie najlepszego modelu w każdej kategorii (zielony background)
  - Przycisk "Odśwież metryki" - przeliczenie na podstawie nowych danych

#### US-016: Eksport prognoz do CSV
- ID: US-016
- Tytuł: Eksport danych prognoz do pliku CSV
- Opis: Jako zalogowany użytkownik chcę wyeksportować prognozy do pliku CSV, aby przeprowadzić własne analizy
- Kryteria akceptacji:
  - Przycisk "Eksport prognoz do CSV" w sekcji prognoz
  - Format CSV: data, RCKiK, grupa_krwi, model, przewidywany_stan, dolna_granica, górna_granica
  - Eksport uwzględnia aktywne filtry
  - Nazwa pliku: mkrew_prognozy_{RCKiK}_{grupa}_{data}.csv

### 5.4. Scraping danych (ADMIN)

#### US-017: Ręczne wywołanie scrapingu dla wszystkich RCKiK
- ID: US-017
- Tytuł: Manualne uruchomienie procesu scrapingu dla wszystkich 21 RCKiK
- Opis: Jako ADMIN chcę ręcznie wywołać scraping wszystkich stron RCKiK, aby natychmiast zaktualizować dane o stanach magazynowych
- Kryteria akceptacji:
  - Przycisk "Trigger scraping - wszystkie RCKiK" w Panelu Admin sekcji Scraping
  - Dialog potwierdzenia: "Czy na pewno chcesz uruchomić scraping dla wszystkich 21 RCKiK?"
  - Po potwierdzeniu:
    - Request POST /api/admin/scraper/trigger-all
    - Wyświetlenie loadera "Trwa scraping..."
    - Progress indicator: "Zakończono X/21 RCKiK"
  - Po zakończeniu: podsumowanie "Scraping zakończony: 21 RCKiK, 19 sukces, 2 błędy"
  - Lista błędów (jeśli wystąpiły) z nazwą RCKiK i przyczyną

#### US-018: Ręczne wywołanie scrapingu dla pojedynczego RCKiK
- ID: US-018
- Tytuł: Manualne uruchomienie procesu scrapingu dla wybranego RCKiK
- Opis: Jako ADMIN chcę ręcznie wywołać scraping dla konkretnego RCKiK, aby zaktualizować dane tylko dla tego centrum
- Kryteria akceptacji:
  - Sekcja "Scraping pojedynczego RCKiK" w Panelu Admin
  - Dropdown wyboru RCKiK (21 opcji)
  - Przycisk "Trigger scraping"
  - Po kliknięciu:
    - Request POST /api/admin/scraper/trigger/{rckikCode}
    - Loader "Trwa scraping RCKiK {nazwa}..."
  - Po zakończeniu: komunikat sukcesu "Scraping RCKiK {nazwa} zakończony: zebrano X rekordów" lub komunikat błędu
  - Automatyczna aktualizacja tabeli logów scrapingu

#### US-019: Przeglądanie logów scrapingu
- ID: US-019
- Tytuł: Przegląd historii operacji scrapingu
- Opis: Jako ADMIN chcę widzieć historię wszystkich operacji scrapingu z informacją o sukcesach i błędach
- Kryteria akceptacji:
  - Tabela "Historia scrapingu" w Panelu Admin
  - Kolumny: Data/czas, RCKiK, Status (SUCCESS/FAILURE), Liczba rekordów, Komunikat błędu (jeśli jest)
  - Sortowanie po dacie (najnowsze pierwsze)
  - Filtrowanie po RCKiK, statusie, dacie
  - Paginacja (20 rekordów na stronę)
  - Kolorowe oznaczenie statusu: zielony (SUCCESS), czerwony (FAILURE)
  - Dane z tabeli scraping_log przez backend API

#### US-020: Automatyczne odświeżanie danych po scrapingu
- ID: US-020
- Tytuł: Automatyczne odświeżenie dashboardu po zakończeniu scrapingu
- Opis: Jako ADMIN chcę, aby dashboard automatycznie odświeżył dane po zakończeniu scrapingu, aby od razu zobaczyć zaktualizowane stany
- Kryteria akceptacji:
  - Po zakończeniu scrapingu (manualnego przez ADMIN):
    - Automatyczne odświeżenie tabeli głównej (górna sekcja dashboardu)
    - Aktualizacja wykresu historycznego (jeśli wyświetlany)
  - Komunikat: "Dane zaktualizowane: ostatni scraping {data/czas}"
  - Możliwość wyłączenia auto-refresh w ustawieniach (checkbox "Auto-refresh po scrapingu")

### 5.5. System alertów

#### US-021: Wyświetlanie listy aktywnych alertów
- ID: US-021
- Tytuł: Przeglądanie listy ostrzeżeń i alertów krytycznych
- Opis: Jako zalogowany użytkownik (ADMIN lub USER_DATA) chcę widzieć listę aktywnych alertów o przewidywanych niedoborach, aby być świadomym potencjalnych problemów
- Kryteria akceptacji:
  - Panel boczny lub sekcja "Alerty" na dashboardzie
  - Lista alertów w formacie: "RCKiK {nazwa}: przewidywany niedobór {grupa} za {X} dni (pewność {Y}%)"
  - Dwa poziomy alertów:
    - Ostrzeżenie (pomarańczowy): Stan niski przewidywany w ciągu 7 dni
    - Krytyczne (czerwony): Stan bardzo niski przewidywany w ciągu 3 dni
  - Sortowanie: krytyczne na górze, potem według daty przewidywanego niedoboru (najbliższe pierwsze)
  - Filtrowanie: "Wszystkie", "Tylko krytyczne", "Tylko ostrzeżenia"
  - Licznik alertów: "12 aktywnych alertów (5 krytycznych, 7 ostrzeżeń)"

#### US-022: Wizualne oznaczenie alertów na wykresach
- ID: US-022
- Tytuł: Wizualne oznaczenie przewidywanych niedoborów na wykresach prognoz
- Opis: Jako zalogowany użytkownik chcę widzieć wizualne oznaczenia alertów na wykresach prognoz, aby szybko zidentyfikować problematyczne okresy
- Kryteria akceptacji:
  - Na wykresie prognozy:
    - Pionowa linia przerywana w dniu przewidywanego niedoboru
    - Ikona ostrzeżenia (⚠️) lub ikona krytyczna (🔴) przy tej dacie
    - Tooltip po najechaniu: "Alert krytyczny: przewidywany stan CRITICALLY_LOW, pewność 82%"
  - Kolorowanie obszaru prognozy:
    - Czerwony obszar gdy prognoza wskazuje stan LOW lub CRITICALLY_LOW
    - Żółty gdy MEDIUM
    - Zielony gdy SATISFACTORY lub OPTIMAL

#### US-023: Generowanie alertów na podstawie prognoz
- ID: US-023
- Tytuł: Automatyczne tworzenie alertów po wygenerowaniu prognozy
- Opis: Jako system chcę automatycznie tworzyć alerty gdy prognoza wskazuje na nadchodzący niedobór, aby użytkownicy byli informowani o potencjalnych problemach
- Kryteria akceptacji:
  - Po wygenerowaniu prognozy system analizuje przewidywane wartości
  - Jeśli w horyzoncie 7 dni prognoza wskazuje stan LOW (2):
    - Tworzony jest alert typu "Ostrzeżenie"
  - Jeśli w horyzoncie 3 dni prognoza wskazuje stan CRITICALLY_LOW (1):
    - Tworzony jest alert typu "Krytyczny"
  - Alert zapisywany w bazie z informacjami: RCKiK, grupa krwi, data przewidywanego niedoboru, poziom alertu, pewność prognozy
  - Alerty automatycznie usuwane gdy data przewidywanego niedoboru minie lub gdy nowa prognoza nie potwierdza niedoboru

### 5.6. Zarządzanie użytkownikami (ADMIN - opcjonalnie w MVP)

#### US-024: Dodawanie nowego użytkownika USER_DATA
- ID: US-024
- Tytuł: Tworzenie nowego konta użytkownika z rolą USER_DATA
- Opis: Jako ADMIN chcę dodać nowego użytkownika z rolą USER_DATA, aby mógł on przeglądać dane i prognozy
- Kryteria akceptacji:
  - Sekcja "Zarządzanie użytkownikami" w Panelu Admin
  - Przycisk "Dodaj użytkownika"
  - Formularz zawiera pola:
    - Username (wymagane, unikalne, 3-50 znaków)
    - Email (wymagane, unikalne, format email)
    - Password (wymagane, min. 8 znaków)
    - Rola: wybór z dropdown (USER_DATA, ADMIN)
  - Walidacja formularza:
    - Sprawdzenie unikalności username i email
    - Sprawdzenie siły hasła
  - Po zapisaniu:
    - Hasło hashowane (BCrypt)
    - Użytkownik zapisany w tabeli users
    - Komunikat sukcesu: "Użytkownik {username} dodany pomyślnie"
  - Lista użytkowników automatycznie odświeżona

#### US-025: Przeglądanie listy użytkowników
- ID: US-025
- Tytuł: Wyświetlenie listy wszystkich użytkowników systemu
- Opis: Jako ADMIN chcę widzieć listę wszystkich użytkowników z informacją o ich rolach i statusie
- Kryteria akceptacji:
  - Tabela użytkowników w Panelu Admin
  - Kolumny: ID, Username, Email, Rola, Data utworzenia, Ostatnie logowanie, Status (enabled/disabled)
  - Sortowanie po username, dacie utworzenia
  - Filtrowanie po roli
  - Każdy wiersz ma przyciski: "Edytuj", "Usuń"
  - Dane z endpointu GET /api/admin/users (do zaimplementowania jeśli funkcjonalność w MVP)

#### US-026: Usuwanie użytkownika
- ID: US-026
- Tytuł: Usunięcie konta użytkownika z systemu
- Opis: Jako ADMIN chcę usunąć konto użytkownika, aby odebrać mu dostęp do systemu
- Kryteria akceptacji:
  - Przycisk "Usuń" przy każdym użytkowniku w tabeli
  - Dialog potwierdzenia: "Czy na pewno chcesz usunąć użytkownika {username}? Ta operacja jest nieodwracalna."
  - Po potwierdzeniu:
    - Request DELETE /api/admin/users/{userId}
    - Użytkownik usunięty z bazy danych
    - Komunikat sukcesu: "Użytkownik {username} usunięty"
  - Niemożliwe usunięcie samego siebie (aktualnie zalogowanego ADMIN)
  - Lista użytkowników automatycznie odświeżona

### 5.7. Konfiguracja i ustawienia

#### US-027: Harmonogram automatycznego scrapingu
- ID: US-027
- Tytuł: Wyświetlenie harmonogramu automatycznego scrapingu
- Opis: Jako ADMIN chcę widzieć harmonogram automatycznego scrapingu, aby wiedzieć kiedy dane są aktualizowane
- Kryteria akceptacji:
  - Sekcja "Harmonogram scrapingu" w Panelu Admin
  - Informacja: "Automatyczny scraping uruchamiany 3 razy dziennie"
  - Lista scheduled jobs:
    - 8:00 - Scraping poranny
    - 14:00 - Scraping południowy
    - 20:00 - Scraping wieczorny
  - Informacja o ostatnim automatycznym scrapingu: "Ostatni automatyczny scraping: 2025-01-15 14:05, status: SUCCESS, 21/21 RCKiK"
  - Informacja o następnym zaplanowanym: "Następny scraping: 2025-01-15 20:00 (za 5h 55min)"

#### US-028: Informacje systemowe i status usług
- ID: US-028
- Tytuł: Wyświetlenie statusu komponentów systemu
- Opis: Jako ADMIN chcę widzieć status wszystkich komponentów systemu (Backend, Scraper, ML Service, Database), aby monitorować ich działanie
- Kryteria akceptacji:
  - Sekcja "Status systemu" w Panelu Admin
  - Tabela komponentów:
    - Nazwa komponentu: Backend API, Scraper Service, ML Service, Database
    - Status: Online/Offline (zielona/czerwona kropka)
    - Wersja
    - Ostatnie sprawdzenie: timestamp
  - Przycisk "Odśwież status"
  -Healthcheck endpoints:
    - Backend: GET /api/health
    - Scraper: GET /api/scraper/health
    - ML: GET /health
    - Database: sprawdzenie przez backend

### 5.8. Scenariusze błędów i edge cases

#### US-029: Obsługa błędu braku danych historycznych
- ID: US-029
- Tytuł: Obsługa sytuacji gdy brak wystarczających danych historycznych do prognozy
- Opis: Jako ADMIN próbujący wygenerować prognozę chcę otrzymać jasny komunikat gdy brak wystarczających danych historycznych, aby zrozumieć dlaczego prognoza nie może być utworzona
- Kryteria akceptacji:
  - Próba wygenerowania prognozy dla RCKiK/grupy z < 10 punktami danych (dla ARIMA):
    - Komunikat błędu: "Niewystarczająca ilość danych historycznych. Minimum wymagane: 10 punktów, dostępne: {X}. Proszę poczekać na zebranie większej ilości danych."
  - Dla innych modeli (Prophet: min 14, LSTM: min 27):
    - Analogiczny komunikat z odpowiednim minimum
  - Sugestia: "Spróbuj użyć modelu ARIMA (wymaga najmniej danych: 10 punktów)"

#### US-030: Obsługa błędu scrapingu
- ID: US-030
- Tytuł: Informowanie użytkownika o niepowodzeniu scrapingu
- Opis: Jako ADMIN wywołujący scraping chcę otrzymać szczegółowy komunikat o błędzie gdy scraping się nie powiedzie, aby zrozumieć przyczynę problemu
- Kryteria akceptacji:
  - Gdy scraping nie powiedzie się:
    - Komunikat błędu z przyczyną: "Scraping RCKiK {nazwa} nieudany: {przyczyna}"
    - Możliwe przyczyny:
      - "Timeout: strona nie odpowiada"
      - "Błąd parsowania: zmieniona struktura strony"
      - "Brak połączenia internetowego"
      - "Strona niedostępna (HTTP 503)"
  - Błąd logowany w tabeli scraping_log z pełnym stack trace
  - Możliwość ponowienia scrapingu (przycisk "Spróbuj ponownie")

#### US-031: Obsługa wygasłego tokenu JWT
- ID: US-031
- Tytuł: Przekierowanie do logowania po wygaśnięciu tokenu
- Opis: Jako zalogowany użytkownik chcę być przekierowany do strony logowania gdy mój token JWT wygaśnie, aby ponownie się zalogować
- Kryteria akceptacji:
  - Próba wywołania chronionego endpointu z wygasłym tokenem zwraca 401 Unauthorized
  - Frontend wykrywa odpowiedź 401
  - Automatyczne przekierowanie do strony logowania
  - Komunikat: "Twoja sesja wygasła. Proszę zalogować się ponownie."
  - Po ponownym zalogowaniu użytkownik wraca do poprzedniej strony (jeśli możliwe)

#### US-032: Obsługa niedostępności ML Service
- ID: US-032
- Tytuł: Informowanie użytkownika o niedostępności usługi ML
- Opis: Jako ADMIN próbujący wygenerować prognozę chcę otrzymać komunikat gdy ML Service jest niedostępny
- Kryteria akceptacji:
  - Backend próbuje wywołać POST http://ml:5000/api/forecast
  - Timeout lub błąd połączenia:
    - Backend zwraca błąd 503 Service Unavailable
    - Komunikat: "Usługa ML tymczasowo niedostępna. Spróbuj ponownie za chwilę."
  - W Panelu Admin w sekcji "Status systemu":
    - ML Service oznaczony jako "Offline" (czerwona kropka)
  - Przycisk "Spróbuj ponownie" przy generowaniu prognozy

#### US-033: Walidacja zakresu dat dla prognoz
- ID: US-033
- Tytuł: Zabezpieczenie przed generowaniem prognoz z nieprawidłowym horyzontem
- Opis: Jako system chcę walidować parametry prognozy, aby zapobiec błędom wynikającym z nieprawidłowych danych wejściowych
- Kryteria akceptacji:
  - Walidacja horyzontu prognozy:
    - ARIMA: max 7 dni (zgodnie z dokumentacją modelu)
    - Prophet: max 30 dni
    - SARIMA: max 14 dni
    - LSTM: max 7 dni
  - Próba wygenerowania prognozy ARIMA z horyzontem 14 dni:
    - Błąd walidacji: "Model ARIMA obsługuje prognozy do 7 dni. Wybierz Prophet lub SARIMA dla dłuższego horyzontu."
  - Walidacja po stronie backendu (przed wysłaniem do ML Service)

## 6. Metryki sukcesu

### 6.1. Kryteria sukcesu technicznego (PoC)

#### 6.1.1. Stabilność systemu
- System działa stabilnie przez minimum 3 miesiące bez krytycznych błędów
- Uptime wszystkich serwisów (Backend, Scraper, ML): > 95%
- Automatyczny scraping wykonywany zgodnie z harmonogramem (3x dziennie) z sukcesem > 90%

#### 6.1.2. Dokładność prognoz
- Accuracy prognoz krótkoterminowych (7 dni): > 70%
  - Mierzone jako % poprawnie przewidzianych kategorii stanów (CRITICALLY_LOW, LOW, MEDIUM, SATISFACTORY, OPTIMAL)
- Recall dla stanów niskich (LOW, CRITICALLY_LOW): > 80%
  - Kluczowe: system musi wykrywać większość potencjalnych niedoborów
- MAE (Mean Absolute Error): < 1.0 (na skali 1-5)
- RMSE (Root Mean Squared Error): < 1.2

#### 6.1.3. Dokładność prognoz średnioterminowych
- Accuracy prognoz 14-dniowych: > 60%
- Recall dla stanów niskich: > 70%

#### 6.1.4. Kompletność danych
- Pokrycie danych: minimum 90% dni ze wszystkich 21 RCKiK ma zebrane dane
- Brak luk w danych dłuższych niż 24h dla > 95% przypadków

#### 6.1.5. Wydajność
- Czas generowania pojedynczej prognozy: < 10 sekund (ARIMA, Prophet, SARIMA), < 30 sekund (LSTM)
- Czas odpowiedzi API (GET endpoints): < 1 sekunda
- Czas batch forecast (168 prognoz): < 15 minut

### 6.2. Kryteria sukcesu biznesowego

#### 6.2.1. Dokumentacja i prezentacja
- Kompletna dokumentacja techniczna systemu (architektura, API, modele ML)
- Raport z ewaluacji modeli zawierający:
  - Porównanie dokładności 4 modeli ML
  - Analiza przypadków sukcesu i porażki prognoz
  - Identyfikacja wzorców sezonowych i cyklicznych
  - Case studies: minimum 5 przykładów poprawnie przewidzianych niedoborów
- Prezentacja gotowa do pokazania przed Narodowym Centrum Krwi (NCK) zawierająca:
  - Problem biznesowy i rozwiązanie
  - Wyniki techniczne (metryki dokładności)
  - Potencjalne korzyści dla systemu krwiodawstwa
  - Roadmap rozwoju (Faza 2: platforma społecznościowa)

#### 6.2.2. Zainteresowanie i feedback
- Prezentacja systemu przed minimum 3 potencjalnymi stakeholderami (NCK, RCKiK, eksperci medyczni)
- Zebranie feedbacku i sugestii rozwoju
- Pozytywna ocena koncepcji i potencjału praktycznego zastosowania

#### 6.2.3. Wartość naukowa
- Publikacja wyników w formie artykułu naukowego lub raportu badawczego
- Udokumentowanie metodologii badawczej (zgodnie z metodologia.md)
- Udostępnienie kodu źródłowego jako open-source (po zakończeniu kursu)

### 6.3. Metryki użyteczności (MVP)

#### 6.3.1. Użyteczność interfejsu
- Dashboard zawiera wszystkie kluczowe informacje na jednej stronie (minimalna ilość kliknięć)
- Czas potrzebny ADMIN na wygenerowanie prognozy wsadowej: < 2 minuty (od logowania do otrzymania wyników)
- Czas potrzebny USER_DATA na znalezienie informacji o stanie konkretnego RCKiK: < 30 sekund

#### 6.3.2. Funkcjonalność alertów
- Średni czas wyprzedzenia alertu krytycznego: minimum 5 dni przed przewidywanym niedoborem
- Fałszywe alarmy: < 30% (alerty które nie potwierdziły się rzeczywistym niedoborem)
- Missed detections: < 20% (rzeczywiste niedobory które nie zostały przewidziane)

### 6.4. Długoterminowe metryki (post-MVP, Faza 2)

Te metryki będą mierzone po wdrożeniu platformy społecznościowej:

- Liczba zarejestrowanych użytkowników (dawców krwi): > 1000 w pierwszych 6 miesiącach
- Engagement rate: > 20% aktywnych użytkowników miesięcznie
- Liczba deklaracji oddania krwi złożonych przez platformę: > 500 w pierwszym roku
- Konwersja deklaracja → faktyczne oddanie krwi: > 40%
- Redukcja liczby sytuacji kryzysowych w RCKiK (mierzona współpracą z NCK): > 15% rok do roku

### 6.5. Warunki sukcesu projektu (definicja "done" dla MVP)

MVP uznajemy za ukończone gdy spełnione są następujące warunki:

1. Wszystkie user stories (US-001 do US-033) zaimplementowane i przetestowane
2. System działa stabilnie przez minimum 30 dni bez krytycznych błędów
3. Zebrano minimum 90 dni danych historycznych ze wszystkich 21 RCKiK
4. Wygenerowano minimum 100 prognoz i porównano je z rzeczywistością
5. Accuracy prognoz 7-dniowych osiągnęła > 70%
6. Dashboard frontend zawiera wszystkie kluczowe wizualizacje (tabela, wykresy, alerty)
7. Dokumentacja techniczna kompletna i aktualna
8. Prezentacja gotowa do pokazania stakeholderom
9. Code review przeprowadzony, brak critical issues
10. Deployment w środowisku Docker Compose działa poprawnie

### 6.6. Metryki monitoringu (ciągłe)

#### 6.6.1. Metryki operacyjne
- Liczba udanych scrapingów dziennie: > 60 (21 RCKiK x 3 scraping/dzień)
- Liczba błędów scrapingu tygodniowo: < 5
- Liczba wygenerowanych prognoz dziennie: śledzone dla analizy użycia
- Liczba aktywnych sesji użytkowników dziennie: śledzone

#### 6.6.2. Metryki jakości danych
- % kompletnych rekordów (wszystkie 8 grup krwi w rekordzie): > 95%
- Czas opóźnienia danych (od publikacji na stronie RCKiK do pojawienia się w systemie): < 2 godziny
- Liczba wykrytych anomalii w danych: śledzone dla manualnej weryfikacji

#### 6.6.3. Metryki alertów
- Liczba wygenerowanych alertów tygodniowo: śledzone
- Stosunek alertów krytycznych do ostrzeżeń: śledzone
- Średni czas życia alertu (od wygenerowania do rozwiązania/wygaśnięcia): śledzone

---

## Podsumowanie

Niniejszy PRD definiuje zakres MVP systemu mkrew2 jako narzędzia do prognozowania i wizualizacji stanów magazynowych krwi w 21 RCKiK w Polsce. MVP koncentruje się na aspektach technicznych (scraping, ML, prognozy, dashboard) i jest skierowany do zamkniętej grupy użytkowników (ADMIN, USER_DATA).

Kluczowe założenia MVP:
- Proof of Concept dla kursu akademickiego 10xdevs 2.0
- Standalone system bez integracji z systemami RCKiK/NCK
- Dane ze stron publicznych (web scraping)
- 4 modele ML: ARIMA, Prophet, SARIMA, LSTM
- Prognozy krótko- i średnioterminowe (1-7 dni, 1-4 tygodnie)
- System alertów (wizualny + lista)
- Walidacja dokładności prognoz
- Eksport do CSV

Platforma społecznościowa dla krwiodawstwa (deklaracje, prośby, komunikacja) jest zaplanowana jako odrębna Faza 2 rozwoju produktu, po udanej walidacji MVP.

Sukces MVP będzie mierzony przez:
- Accuracy > 70% dla prognoz 7-dniowych
- Recall > 80% dla stanów niskich
- Stabilność przez 3 miesiące
- Gotowość do prezentacji przed NCK/RCKiK z udokumentowanymi wynikami
