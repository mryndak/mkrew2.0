# Backend API Deployment to GCP Cloud Run

Dokumentacja wdrażania Backend API na Google Cloud Platform jako Cloud Run service z integracją Cloud SQL i ML service.

## 📋 Spis treści

- [Architektura](#architektura)
- [Wymagania](#wymagania)
- [Setup infrastruktury](#setup-infrastruktury)
- [Wdrażanie Backend](#wdrażanie-backend)
- [Integracje](#integracje)
- [Bezpieczeństwo](#bezpieczeństwo)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Architektura

```
┌──────────────┐
│   Frontend   │
│  (users)     │
└──────┬───────┘
       │ HTTPS
       ▼
┌──────────────────┐
│  Backend API     │
│  Cloud Run       │ (public with JWT)
│  :8080           │
└──┬───┬───┬───────┘
   │   │   │
   │   │   └─────► ┌─────────────┐
   │   │           │  ML Service │ (authenticated)
   │   │           │  Cloud Run  │
   │   │           └─────────────┘
   │   │
   │   └───────────► ┌─────────────┐
   │                 │  Scraper    │ (API key)
   │                 │  Cloud Run  │
   │                 └─────────────┘
   │
   └─────────────────► ┌─────────────┐
                       │  Cloud SQL  │ (PostgreSQL)
                       │  PostgreSQL │
                       └─────────────┘
```

## Wymagania

### Infrastruktura (musi być wcześniej wdrożona)

1. **Cloud SQL PostgreSQL** - baza danych
   ```bash
   cd db/terraform
   terraform apply
   ```

2. **ML Service** - serwis prognoz
   ```bash
   cd ml
   ./scripts/setup-infrastructure.sh PROJECT_ID dev
   ./scripts/deploy-ml.sh dev
   ```

### Narzędzia

- Google Cloud SDK (gcloud)
- Docker (opcjonalnie)
- Terraform >= 1.0 (opcjonalnie)
- Uprawnienia GCP:
  - `roles/run.admin`
  - `roles/cloudsql.client`
  - `roles/secretmanager.admin`
  - `roles/iam.serviceAccountAdmin`

### Koszty (europa-central2)

| Environment | CPU | Memory | Instances | Koszt/miesiąc |
|-------------|-----|--------|-----------|---------------|
| **dev** | 2 | 1Gi | 0-5 | ~$20-40 |
| **staging** | 2 | 2Gi | 1-10 | ~$50-100 |
| **prod** | 4 | 2Gi | 2-20 | ~$150-300 |

## Setup infrastruktury

### Opcja 1: Automatyczny skrypt (Zalecana)

```bash
cd backend

# Nadanie uprawnień
chmod +x scripts/setup-backend-infrastructure.sh

# Uruchomienie
./scripts/setup-backend-infrastructure.sh YOUR_PROJECT_ID dev
```

**Co zostanie utworzone:**
- ✅ Service account (mkrew-backend-sa)
- ✅ JWT Secret (64 bytes, Secret Manager)
- ✅ Scraper API Key (48 bytes, Secret Manager)
- ✅ IAM permissions (Cloud SQL, Secret Manager, Cloud Run)
- ✅ Cloud Build permissions

### Opcja 2: Terraform

```bash
cd backend/terraform

terraform init

terraform plan \
  -var="project_id=YOUR_PROJECT_ID" \
  -var="environment=dev" \
  -var="cloud_sql_instance=PROJECT:REGION:INSTANCE" \
  -var="ml_service_url=https://mkrew-ml-dev-xxx.run.app"

terraform apply \
  -var="project_id=YOUR_PROJECT_ID" \
  -var="environment=dev" \
  -var="cloud_sql_instance=PROJECT:REGION:INSTANCE" \
  -var="ml_service_url=https://mkrew-ml-dev-xxx.run.app"

# Wyświetl secrets (zapisz!)
terraform output -raw jwt_secret
terraform output -raw scraper_api_key
```

### Opcja 3: Ręczna konfiguracja

<details>
<summary>Kliknij aby rozwinąć</summary>

```bash
PROJECT_ID="your-project-id"

# 1. Enable APIs
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  sqladmin.googleapis.com

# 2. Create service account
gcloud iam service-accounts create mkrew-backend-sa \
  --display-name="Backend Service Account" \
  --project="${PROJECT_ID}"

# 3. Generate secrets
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
SCRAPER_API_KEY=$(openssl rand -base64 48 | tr -d '\n')

echo -n "$JWT_SECRET" | gcloud secrets create mkrew-jwt-secret \
  --data-file=- \
  --replication-policy=automatic \
  --project="${PROJECT_ID}"

echo -n "$SCRAPER_API_KEY" | gcloud secrets create mkrew-scraper-api-key \
  --data-file=- \
  --replication-policy=automatic \
  --project="${PROJECT_ID}"

# 4. Grant permissions
gcloud secrets add-iam-policy-binding mkrew-jwt-secret \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project="${PROJECT_ID}"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

</details>

## Wdrażanie Backend

### Przed wdrożeniem - Checklist

- [ ] Cloud SQL PostgreSQL wdrożony i działa
- [ ] ML Service wdrożony (opcjonalnie)
- [ ] Backend infrastructure setup wykonany
- [ ] Secrets utworzone w Secret Manager

### Metoda 1: Skrypt deployment (Zalecana)

```bash
cd backend

# Nadanie uprawnień
chmod +x scripts/deploy-backend.sh

# Wdrożenie do dev
export GCP_PROJECT_ID="your-project-id"
./scripts/deploy-backend.sh dev

# Wdrożenie do staging
./scripts/deploy-backend.sh staging

# Wdrożenie do prod
./scripts/deploy-backend.sh prod
```

**Skrypt automatycznie:**
1. Buduje Docker image
2. Pushuje do Container Registry
3. **Uruchamia migracje Liquibase**
4. Deployuje na Cloud Run
5. Konfiguruje Cloud SQL integration
6. Nadaje uprawnienia ML service
7. Testuje health endpoint

### Metoda 2: Cloud Build (ręcznie)

```bash
# Pobierz Cloud SQL connection name
CLOUD_SQL_INSTANCE=$(gcloud sql instances describe mkrew-postgres-europe-central2 \
  --format='value(connectionName)')

# Pobierz ML Service URL
ML_SERVICE_URL=$(gcloud run services describe mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(status.url)')

# Deploy
gcloud builds submit \
  --config=backend/cloudbuild.yaml \
  --project=YOUR_PROJECT_ID \
  --substitutions=\
_ENVIRONMENT=dev,\
_REGION=europe-central2,\
_CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE}",\
_DB_NAME=mkrew,\
_DB_USER=mkrew,\
_ML_SERVICE_URL="${ML_SERVICE_URL}",\
_MEMORY=1Gi,\
_CPU=2,\
_MIN_INSTANCES=1,\
_MAX_INSTANCES=10
```

### Metoda 3: Lokalne build + gcloud

```bash
cd backend

# Build
docker build -t gcr.io/YOUR_PROJECT_ID/mkrew-backend:latest .

# Push
docker push gcr.io/YOUR_PROJECT_ID/mkrew-backend:latest

# Deploy
gcloud run deploy mkrew-backend-dev \
  --image=gcr.io/YOUR_PROJECT_ID/mkrew-backend:latest \
  --region=europe-central2 \
  --platform=managed \
  --allow-unauthenticated \
  --service-account=mkrew-backend-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --add-cloudsql-instances=YOUR_CLOUD_SQL_INSTANCE \
  --set-secrets=JWT_SECRET=mkrew-jwt-secret:latest,SCRAPER_API_KEY=mkrew-scraper-api-key:latest,ML_API_KEY=mkrew-ml-api-key:latest \
  --memory=1Gi \
  --cpu=2
```

## Integracje

### Cloud SQL (PostgreSQL)

**Konfiguracja połączenia:**

Backend używa Cloud SQL Connector:
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql:///mkrew?cloudSqlInstance=PROJECT:REGION:INSTANCE&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

**Migrations:**
- Liquibase migrations uruchamiane automatycznie podczas deployment
- Changelog: `/db/changelog/db.changelog-master.yaml`

### ML Service (Cloud Run)

**Konfiguracja:**
```bash
# Backend automatycznie otrzymuje:
ML_SERVICE_URL=https://mkrew-ml-dev-xxx.run.app
ML_API_KEY=<from Secret Manager>
```

**Backend wywołuje ML przez:**
1. Cloud Run service-to-service auth (IAM)
2. API Key w nagłówku `X-API-Key`

### Scraper Service (opcjonalnie)

**Konfiguracja:**
```bash
SCRAPER_SERVICE_URL=http://mkrew-scraper-dev:8080
SCRAPER_API_KEY=<from Secret Manager>
```

## Bezpieczeństwo

### Warstwy zabezpieczeń

1. **JWT Authentication**
   - Spring Security + JWT tokens
   - BCrypt password hashing
   - Token expiration: 24h

2. **Role-Based Access Control (RBAC)**
   - ADMIN: Full access
   - USER_DATA: Read-only

3. **Service Account**
   - `mkrew-backend-sa` z minimal permissions
   - Cloud SQL Client role
   - Secret Manager accessor

4. **API Keys**
   - Scraper API Key (dla komunikacji z scraper)
   - ML API Key (dla komunikacji z ML service)
   - Przechowywane w Secret Manager

5. **Network Security**
   - HTTPS tylko
   - Cloud SQL private connection
   - ML service authenticated

### Domyślni użytkownicy

```sql
-- Admin
username: admin
password: admin123
role: ADMIN

-- User
username: user
password: user123
role: USER_DATA
```

⚠️ **Zmień hasła w produkcji!**

## Monitoring

### Health Checks

```bash
BACKEND_URL="https://mkrew-backend-dev-xxx.run.app"

# Spring Boot Actuator health
curl $BACKEND_URL/actuator/health

# Example response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Logi

```bash
# Tail logs
gcloud run services logs tail mkrew-backend-dev \
  --region=europe-central2

# Filtruj błędy
gcloud run services logs read mkrew-backend-dev \
  --region=europe-central2 \
  --log-filter='severity>=ERROR' \
  --limit=100

# Logs z konkretnego czasu
gcloud run services logs read mkrew-backend-dev \
  --region=europe-central2 \
  --log-filter='timestamp>="2025-01-07T10:00:00Z"'
```

### Metryki Cloud Run

```bash
# Opis service
gcloud run services describe mkrew-backend-dev \
  --region=europe-central2

# Revisions (wersje)
gcloud run revisions list \
  --service=mkrew-backend-dev \
  --region=europe-central2

# Traffic split
gcloud run services describe mkrew-backend-dev \
  --region=europe-central2 \
  --format='value(status.traffic)'
```

### Cloud Console Dashboards

- **Cloud Run:** https://console.cloud.google.com/run
- **Metrics:** Request count, latency, error rate, CPU, Memory
- **Logs:** https://console.cloud.google.com/logs
- **Traces:** https://console.cloud.google.com/traces

## API Testing

### Login i JWT Token

```bash
BACKEND_URL="https://mkrew-backend-dev-xxx.run.app"

# Login jako admin
TOKEN=$(curl -s -X POST "$BACKEND_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.token')

echo "JWT Token: $TOKEN"
```

### Wywołanie zabezpieczonego endpoint

```bash
# Get blood inventory data
curl -X GET "$BACKEND_URL/api/blood-inventory" \
  -H "Authorization: Bearer $TOKEN"

# Trigger scraping (ADMIN only)
curl -X POST "$BACKEND_URL/api/admin/scraper/trigger" \
  -H "Authorization: Bearer $TOKEN"

# Create forecast (ADMIN only)
curl -X POST "$BACKEND_URL/api/forecast/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rckikId": 1,
    "bloodType": "A_PLUS",
    "modelType": "ARIMA",
    "forecastHorizonDays": 7
  }'
```

## Database Migrations

### Liquibase Commands

Migrations są uruchamiane automatycznie podczas deployment. Ręczne operacje:

```bash
# Status migracji
docker run --rm \
  --network=cloudbuild \
  gcr.io/YOUR_PROJECT_ID/mkrew-liquibase:latest \
  --url="jdbc:postgresql://CLOUD_SQL_PROXY:5432/mkrew" \
  --username=mkrew \
  --password=PASSWORD \
  --changelog-file=changelog/db.changelog-master.yaml \
  status

# Rollback (1 changeset)
docker run --rm \
  --network=cloudbuild \
  gcr.io/YOUR_PROJECT_ID/mkrew-liquibase:latest \
  --url="jdbc:postgresql://CLOUD_SQL_PROXY:5432/mkrew" \
  --username=mkrew \
  --password=PASSWORD \
  --changelog-file=changelog/db.changelog-master.yaml \
  rollbackCount 1
```

## Rollback

### Rollback do poprzedniej wersji

```bash
# Lista rewizji
gcloud run revisions list \
  --service=mkrew-backend-dev \
  --region=europe-central2

# Rollback
gcloud run services update-traffic mkrew-backend-dev \
  --region=europe-central2 \
  --to-revisions=mkrew-backend-dev-00001-abc=100
```

### Canary Deployment

```bash
# 90% old, 10% new
gcloud run services update-traffic mkrew-backend-dev \
  --region=europe-central2 \
  --to-revisions=mkrew-backend-dev-00001-abc=90,mkrew-backend-dev-00002-xyz=10
```

## Troubleshooting

### Problem: "Cloud SQL connection failed"

```bash
# Sprawdź czy backend ma Cloud SQL Client role
gcloud projects get-iam-policy YOUR_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:mkrew-backend-sa@*"

# Sprawdź Cloud SQL instance
gcloud sql instances describe mkrew-postgres-europe-central2 \
  --format="value(state)"

# Sprawdź logi backend
gcloud run services logs read mkrew-backend-dev \
  --region=europe-central2 \
  --log-filter='textPayload=~"sql"' \
  --limit=50
```

### Problem: "ML Service 403 Forbidden"

```bash
# Sprawdź czy backend ma run.invoker na ML service
gcloud run services get-iam-policy mkrew-ml-dev \
  --region=europe-central2

# Dodaj uprawnienie
gcloud run services add-iam-policy-binding mkrew-ml-dev \
  --region=europe-central2 \
  --member="serviceAccount:mkrew-backend-sa@PROJECT.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

### Problem: "Out of memory"

```bash
# Zwiększ memory
gcloud run services update mkrew-backend-dev \
  --region=europe-central2 \
  --memory=2Gi

# Sprawdź JVM heap
gcloud run services logs read mkrew-backend-dev \
  --region=europe-central2 \
  --log-filter='textPayload=~"OutOfMemory"'
```

### Problem: "Migrations failed"

```bash
# Sprawdź Liquibase lock
# Połącz się z Cloud SQL przez proxy
gcloud sql connect mkrew-postgres-europe-central2 \
  --user=mkrew \
  --database=mkrew

# W psql:
SELECT * FROM DATABASECHANGELOGLOCK;

# Usuń lock jeśli potrzebne
DELETE FROM DATABASECHANGELOGLOCK WHERE ID=1;
```

## Skalowanie

### Auto-scaling

Cloud Run automatycznie skaluje na podstawie:
- Request load
- CPU usage
- Memory usage
- Concurrency (80 requests/instance)

### Konfiguracja dla różnych środowisk

**Development:**
```bash
--min-instances=0  # Cold start OK
--max-instances=5
--memory=1Gi
--cpu=2
```

**Production:**
```bash
--min-instances=2  # Always warm
--max-instances=20
--memory=2Gi
--cpu=4
--cpu-boost  # Faster response
```

## Continuous Deployment

### GitHub Actions

```yaml
# .github/workflows/deploy-backend.yml
name: Deploy Backend
on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - 'db/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: google-github-actions/auth@v1
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}
      - name: Deploy
        run: |
          gcloud builds submit \
            --config=backend/cloudbuild.yaml \
            --substitutions=_ENVIRONMENT=prod
```

### Cloud Build Trigger

```bash
gcloud builds triggers create github \
  --name="mkrew-backend-deploy" \
  --repo-name="mkrew2" \
  --repo-owner="YOUR_GITHUB_USERNAME" \
  --branch-pattern="^main$" \
  --build-config="backend/cloudbuild.yaml" \
  --included-files="backend/**,db/**" \
  --substitutions=_ENVIRONMENT=prod
```

## Przydatne komendy

```bash
# Status wszystkich services
gcloud run services list --region=europe-central2

# Szczegóły backend
gcloud run services describe mkrew-backend-dev --region=europe-central2

# Update environment variable
gcloud run services update mkrew-backend-dev \
  --region=europe-central2 \
  --set-env-vars="NEW_VAR=value"

# Update secret
gcloud run services update mkrew-backend-dev \
  --region=europe-central2 \
  --update-secrets="JWT_SECRET=mkrew-jwt-secret:latest"

# Scale
gcloud run services update mkrew-backend-dev \
  --region=europe-central2 \
  --min-instances=2 \
  --max-instances=20

# Delete service
gcloud run services delete mkrew-backend-dev --region=europe-central2
```

## Referencje

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud SQL for PostgreSQL](https://cloud.google.com/sql/docs/postgres)
- [Spring Boot on Cloud Run](https://cloud.google.com/run/docs/quickstarts/build-and-deploy/deploy-java-service)
- [Liquibase Documentation](https://docs.liquibase.com)

---

**Ostatnia aktualizacja:** 2025-01-07
**Wersja:** 1.0
