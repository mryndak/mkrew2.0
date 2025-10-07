# ML Service Deployment to GCP Cloud Run

Dokumentacja wdrażania serwisu ML (Machine Learning) na Google Cloud Platform jako Cloud Run service z zabezpieczeniami.

## 📋 Spis treści

- [Architektura](#architektura)
- [Wymagania](#wymagania)
- [Setup infrastruktury](#setup-infrastruktury)
- [Wdrażanie ML service](#wdrażanie-ml-service)
- [Konfiguracja Backend](#konfiguracja-backend)
- [Bezpieczeństwo](#bezpieczeństwo)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Architektura

```
┌──────────────┐
│   Backend    │
│  Cloud Run   │ (authenticated)
│  :8081       │
└──────┬───────┘
       │ HTTPS + API Key
       │ + Cloud Run Invoker
       ▼
┌──────────────┐
│  ML Service  │
│  Cloud Run   │ (no public access)
│  :8080       │
└──────────────┘
```

**Zabezpieczenia:**
1. **Cloud Run Authentication** - ML service wymaga authentication (--no-allow-unauthenticated)
2. **IAM Cloud Run Invoker** - Tylko backend service account może wywołać ML service
3. **API Key** - Dodatkowa warstwa zabezpieczeń przez X-API-Key header
4. **Internal Traffic** - ML service dostępny tylko z VPC i load balancer

## Wymagania

### Narzędzia

- **Google Cloud SDK (gcloud)** - najnowsza wersja
- **Docker** (do lokalnego testowania)
- **Terraform** >= 1.0 (opcjonalne)
- Uprawnienia GCP:
  - `roles/run.admin`
  - `roles/iam.serviceAccountAdmin`
  - `roles/secretmanager.admin`
  - `roles/cloudbuild.builds.editor`

### Koszty (europa-central2)

| Zasób | Specyfikacja | Koszt/miesiąc (szacunkowy) |
|-------|--------------|----------------------------|
| Cloud Run (dev) | 2 vCPU, 2GB RAM, 0-3 inst | ~$15-30 |
| Cloud Run (prod) | 4 vCPU, 4GB RAM, 2-10 inst | ~$100-200 |
| Container Registry | Storage + bandwidth | ~$5 |
| Secret Manager | 2 secrets | ~$0.50 |

**Uwaga:** Cloud Run ma 2 million requests free tier/miesiąc.

## Setup infrastruktury

### Opcja 1: Automatyczny skrypt (Zalecana)

```bash
cd ml

# Nadanie uprawnień
chmod +x scripts/setup-infrastructure.sh

# Uruchomienie setup
./scripts/setup-infrastructure.sh YOUR_PROJECT_ID dev
```

**Skrypt wykona:**
1. ✅ Włączenie wymaganych APIs
2. ✅ Utworzenie service accounts (ML + Backend)
3. ✅ Wygenerowanie bezpiecznego API key (64 bytes)
4. ✅ Zapisanie API key w Secret Manager
5. ✅ Konfiguracja IAM permissions
6. ✅ Cloud Build permissions

### Opcja 2: Terraform

```bash
cd ml/terraform

# Inicjalizacja
terraform init

# Podgląd zmian
terraform plan -var="project_id=YOUR_PROJECT_ID" -var="environment=dev"

# Utworzenie infrastruktury
terraform apply -var="project_id=YOUR_PROJECT_ID" -var="environment=dev"

# Wyświetl API key (zapisz dla backend!)
terraform output -raw ml_api_key
```

### Opcja 3: Ręczna konfiguracja

<details>
<summary>Kliknij aby rozwinąć instrukcje ręczne</summary>

```bash
# 1. Włącz APIs
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  containerregistry.googleapis.com

# 2. Utwórz service accounts
gcloud iam service-accounts create mkrew-ml-sa \
  --display-name="ML Service Account"

gcloud iam service-accounts create mkrew-backend-sa \
  --display-name="Backend Service Account"

# 3. Wygeneruj API key
ML_API_KEY=$(openssl rand -base64 48)
echo "$ML_API_KEY"  # ZAPISZ TO!

# 4. Zapisz w Secret Manager
echo -n "$ML_API_KEY" | gcloud secrets create mkrew-ml-api-key \
  --data-file=- \
  --replication-policy=automatic

# 5. Nadaj uprawnienia
PROJECT_ID=$(gcloud config get-value project)

# ML może czytać własny secret
gcloud secrets add-iam-policy-binding mkrew-ml-api-key \
  --member="serviceAccount:mkrew-ml-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Backend może czytać ML secret
gcloud secrets add-iam-policy-binding mkrew-ml-api-key \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Cloud Build może deployować
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"
```

</details>

## Wdrażanie ML service

### Metoda 1: Skrypt deployment (Zalecana)

```bash
cd ml

# Nadanie uprawnień
chmod +x scripts/deploy-ml.sh

# Wdrożenie do dev
export GCP_PROJECT_ID="your-project-id"
./scripts/deploy-ml.sh dev

# Wdrożenie do staging
./scripts/deploy-ml.sh staging

# Wdrożenie do production
./scripts/deploy-ml.sh prod
```

**Skrypt automatycznie:**
- Buduje Docker image
- Pushuje do Container Registry
- Deployuje na Cloud Run z odpowiednią konfiguracją
- Konfiguruje authentication
- Nadaje uprawnienia backend
- Testuje health endpoint

### Metoda 2: Cloud Build (ręcznie)

```bash
gcloud builds submit \
  --config=ml/cloudbuild.yaml \
  --project=YOUR_PROJECT_ID \
  --substitutions=\
_ENVIRONMENT=dev,\
_REGION=europe-central2,\
_MEMORY=2Gi,\
_CPU=2,\
_MIN_INSTANCES=0,\
_MAX_INSTANCES=3
```

### Metoda 3: Lokalne build + gcloud deploy

```bash
cd ml

# Build obrazu
docker build -t gcr.io/YOUR_PROJECT_ID/mkrew-ml:latest .

# Push do GCR
docker push gcr.io/YOUR_PROJECT_ID/mkrew-ml:latest

# Deploy na Cloud Run
gcloud run deploy mkrew-ml-dev \
  --image=gcr.io/YOUR_PROJECT_ID/mkrew-ml:latest \
  --region=europe-central2 \
  --platform=managed \
  --no-allow-unauthenticated \
  --service-account=mkrew-ml-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --set-secrets=ML_API_KEY=mkrew-ml-api-key:latest \
  --memory=2Gi \
  --cpu=2 \
  --timeout=300s \
  --max-instances=10 \
  --min-instances=1
```

## Konfiguracja Backend

Po wdrożeniu ML service, skonfiguruj backend:

### 1. Pobierz ML Service URL

```bash
ML_URL=$(gcloud run services describe mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(status.url)')

echo "ML Service URL: $ML_URL"
```

### 2. Pobierz ML API Key

```bash
ML_API_KEY=$(gcloud secrets versions access latest --secret=mkrew-ml-api-key)
echo "ML API Key: $ML_API_KEY"
```

### 3. Zaktualizuj backend configuration

**Dla Cloud Run (backend):**

```bash
gcloud run services update mkrew-backend-dev \
  --set-env-vars="ML_SERVICE_URL=${ML_URL}" \
  --set-secrets="ML_API_KEY=mkrew-ml-api-key:latest" \
  --region=europe-central2
```

**Dla lokalnego developmentu:**

```bash
# backend/src/main/resources/application.properties
ml.service.url=${ML_URL}
ml.api.key=${ML_API_KEY}
```

### 4. Nadaj uprawnienie backend do wywołania ML

```bash
gcloud run services add-iam-policy-binding mkrew-ml-dev \
  --region=europe-central2 \
  --member="serviceAccount:mkrew-backend-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

## Bezpieczeństwo

### Warstwy zabezpieczeń

1. **Cloud Run Authentication**
   - ML service: `--no-allow-unauthenticated`
   - Tylko authenticated requests
   - IAM Cloud Run Invoker role wymagany

2. **Service Account**
   - Backend używa `mkrew-backend-sa`
   - ML używa `mkrew-ml-sa`
   - Principle of least privilege

3. **API Key**
   - Dodatkowa warstwa przez `X-API-Key` header
   - 64-byte losowy klucz
   - Przechowywany w Secret Manager

4. **Network Isolation**
   - `--ingress=internal-and-cloud-load-balancing`
   - Brak publicznego dostępu
   - Tylko VPC i load balancer

### Testowanie bezpieczeństwa

```bash
ML_URL="https://mkrew-ml-dev-xxxxx-uc.a.run.app"

# Test 1: Bez autentykacji (powinno zwrócić 403)
curl -X POST "${ML_URL}/api/forecast" \
  -H "Content-Type: application/json"
# Expected: 403 Forbidden

# Test 2: Health endpoint (public, powinno działać)
curl "${ML_URL}/health"
# Expected: 200 OK

# Test 3: Z backend service account (powinno działać)
# Backend automatycznie używa service account token
```

## Monitoring

### Logi Cloud Run

```bash
# Logi ML service
gcloud run services logs read mkrew-ml-dev \
  --region=europe-central2 \
  --limit=50

# Follow logs (real-time)
gcloud run services logs tail mkrew-ml-dev \
  --region=europe-central2

# Filtruj błędy
gcloud run services logs read mkrew-ml-dev \
  --region=europe-central2 \
  --log-filter='severity>=ERROR'
```

### Metryki

```bash
# Opis service
gcloud run services describe mkrew-ml-dev \
  --region=europe-central2

# Rewizje (versions)
gcloud run revisions list \
  --service=mkrew-ml-dev \
  --region=europe-central2
```

### Cloud Console

- **Cloud Run:** https://console.cloud.google.com/run
- **Metryki:** Request count, latency, error rate, CPU, Memory
- **Logs:** Integrated with Cloud Logging
- **Tracing:** Cloud Trace integration

### Alerty (opcjonalne)

```bash
# Utwórz alert policy dla error rate > 5%
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="ML Service High Error Rate" \
  --condition-display-name="Error rate > 5%" \
  --condition-threshold-value=0.05 \
  --condition-threshold-duration=300s
```

## Rollback

### Rollback do poprzedniej wersji

```bash
# Lista rewizji
gcloud run revisions list \
  --service=mkrew-ml-dev \
  --region=europe-central2

# Rollback do konkretnej rewizji
gcloud run services update-traffic mkrew-ml-dev \
  --region=europe-central2 \
  --to-revisions=mkrew-ml-dev-00001-abc=100
```

### Canary Deployment

```bash
# 90% traffic do starej wersji, 10% do nowej
gcloud run services update-traffic mkrew-ml-dev \
  --region=europe-central2 \
  --to-revisions=mkrew-ml-dev-00001-abc=90,mkrew-ml-dev-00002-xyz=10
```

## Skalowanie

### Auto-scaling configuration

Cloud Run automatycznie skaluje na podstawie:
- Request load
- CPU usage
- Memory usage

**Konfiguracja:**

```bash
gcloud run services update mkrew-ml-dev \
  --region=europe-central2 \
  --min-instances=1 \
  --max-instances=10 \
  --concurrency=80 \
  --cpu-throttling \
  --memory=4Gi \
  --cpu=4
```

### Cold start optimization

```bash
# Minimum 1 instancja (eliminuje cold start)
gcloud run services update mkrew-ml-dev \
  --region=europe-central2 \
  --min-instances=1

# CPU always allocated (szybsze przetwarzanie)
gcloud run services update mkrew-ml-dev \
  --region=europe-central2 \
  --cpu-boost
```

## Troubleshooting

### Problem: "Service account does not have permission"

```bash
# Sprawdź uprawnienia backend
gcloud run services get-iam-policy mkrew-ml-dev \
  --region=europe-central2

# Dodaj uprawnienie
gcloud run services add-iam-policy-binding mkrew-ml-dev \
  --region=europe-central2 \
  --member="serviceAccount:mkrew-backend-sa@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

### Problem: "Container failed to start"

```bash
# Sprawdź logi
gcloud run services logs read mkrew-ml-dev \
  --region=europe-central2 \
  --limit=100

# Sprawdź zmienne środowiskowe
gcloud run services describe mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(spec.template.spec.containers[0].env)'

# Sprawdź secrets
gcloud run services describe mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(spec.template.spec.containers[0].env)'
```

### Problem: "Out of memory"

```bash
# Zwiększ memory
gcloud run services update mkrew-ml-dev \
  --region=europe-central2 \
  --memory=4Gi

# Sprawdź użycie
gcloud run services describe mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(status.latestReadyRevisionName)'
```

### Problem: "Timeout"

```bash
# Zwiększ timeout (max 3600s)
gcloud run services update mkrew-ml-dev \
  --region=europe-central2 \
  --timeout=600s
```

## Continuous Deployment

### GitHub Actions (opcjonalnie)

```yaml
# .github/workflows/deploy-ml.yml
name: Deploy ML Service
on:
  push:
    branches: [main]
    paths:
      - 'ml/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: google-github-actions/auth@v1
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}
      - uses: google-github-actions/setup-gcloud@v1
      - name: Deploy
        run: |
          gcloud builds submit \
            --config=ml/cloudbuild.yaml \
            --substitutions=_ENVIRONMENT=prod
```

### Cloud Build Trigger

```bash
# Trigger na push do main (zmiany w ml/)
gcloud builds triggers create github \
  --name="mkrew-ml-deploy" \
  --repo-name="mkrew2" \
  --repo-owner="YOUR_GITHUB_USERNAME" \
  --branch-pattern="^main$" \
  --build-config="ml/cloudbuild.yaml" \
  --included-files="ml/**" \
  --substitutions=_ENVIRONMENT=prod,_REGION=europe-central2
```

## Lokalne testowanie

### Docker Compose

```bash
# Uruchom lokalnie z docker-compose
cd mkrew2
docker-compose up ml

# ML service dostępny na http://localhost:5000
```

### Test z Cloud Run Proxy

```bash
# Proxy do Cloud Run service (wymaga auth)
gcloud run services proxy mkrew-ml-dev \
  --region=europe-central2 \
  --port=8080

# Test
curl http://localhost:8080/health
```

## Przydatne komendy

```bash
# Status wszystkich Cloud Run services
gcloud run services list --region=europe-central2

# Szczegóły ML service
gcloud run services describe mkrew-ml-dev --region=europe-central2

# Traffic split
gcloud run services describe mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(status.traffic)'

# Usuń service
gcloud run services delete mkrew-ml-dev --region=europe-central2

# Usuń wszystkie stare rewizje (zostaw 3 ostatnie)
gcloud run revisions list \
  --service=mkrew-ml-dev \
  --region=europe-central2 \
  --format='value(metadata.name)' \
  --sort-by='~metadata.creationTimestamp' \
  | tail -n +4 \
  | xargs -I {} gcloud run revisions delete {} --region=europe-central2 --quiet
```

## Referencje

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud Run Authentication](https://cloud.google.com/run/docs/authenticating/service-to-service)
- [Secret Manager](https://cloud.google.com/secret-manager/docs)
- [Cloud Build](https://cloud.google.com/build/docs)

---

**Ostatnia aktualizacja:** 2025-01-07
**Wersja:** 1.0
