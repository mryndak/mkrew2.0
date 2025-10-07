# Database Deployment to GCP

Dokumentacja wdrażania bazy danych PostgreSQL i migracji Liquibase na Google Cloud Platform.

## 📋 Spis treści

- [Wymagania](#wymagania)
- [Konfiguracja Cloud SQL](#konfiguracja-cloud-sql)
- [Wdrażanie migracji](#wdrażanie-migracji)
- [Rollback](#rollback)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Wymagania

### Narzędzia

- **Google Cloud SDK (gcloud)** - najnowsza wersja
- **Terraform** >= 1.0 (opcjonalne, do tworzenia infrastruktury)
- **Docker** (do lokalnego testowania)
- Uprawnienia GCP:
  - `roles/cloudsql.admin`
  - `roles/secretmanager.admin`
  - `roles/cloudbuild.builds.editor`
  - `roles/iam.serviceAccountUser`

### Instalacja gcloud

```bash
# Instalacja gcloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Autoryzacja
gcloud auth login

# Ustawienie projektu
gcloud config set project YOUR_PROJECT_ID
```

## Konfiguracja Cloud SQL

### Opcja 1: Terraform (Zalecana)

```bash
cd db/terraform

# Inicjalizacja Terraform
terraform init

# Podgląd zmian
terraform plan -var="project_id=YOUR_PROJECT_ID"

# Utworzenie infrastruktury
terraform apply -var="project_id=YOUR_PROJECT_ID"

# Zapisanie outputs
terraform output -json > outputs.json
```

**Co zostanie utworzone:**
- Cloud SQL PostgreSQL 16 instancja
- Baza danych `mkrew`
- Użytkownik `mkrew` z bezpiecznym hasłem
- Secret Manager z hasłem do bazy
- Uprawnienia dla Cloud Build
- Automatyczne backupy (retention: 7 dni)
- Point-in-Time Recovery

### Opcja 2: Ręczna konfiguracja (gcloud)

```bash
# 1. Utworzenie instancji Cloud SQL
gcloud sql instances create mkrew-postgres-europe-central2 \
  --database-version=POSTGRES_16 \
  --tier=db-f1-micro \
  --region=europe-central2 \
  --backup-start-time=03:00 \
  --enable-point-in-time-recovery

# 2. Utworzenie bazy danych
gcloud sql databases create mkrew \
  --instance=mkrew-postgres-europe-central2

# 3. Utworzenie użytkownika (wygeneruj bezpieczne hasło)
gcloud sql users create mkrew \
  --instance=mkrew-postgres-europe-central2 \
  --password=YOUR_SECURE_PASSWORD

# 4. Zapisanie hasła w Secret Manager
echo -n "YOUR_SECURE_PASSWORD" | gcloud secrets create mkrew-db-password \
  --data-file=- \
  --replication-policy=automatic

# 5. Nadanie uprawnień Cloud Build
PROJECT_ID=$(gcloud config get-value project)

gcloud secrets add-iam-policy-binding mkrew-db-password \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

## Konfiguracja Cloud Build

### Włączenie API

```bash
gcloud services enable cloudbuild.googleapis.com
gcloud services enable sqladmin.googleapis.com
gcloud services enable secretmanager.googleapis.com
```

### Trigger dla automatycznego wdrażania

```bash
# Trigger uruchamiany przy push do main/zmianach w db/
gcloud builds triggers create github \
  --name="mkrew-db-migrations" \
  --repo-name="mkrew2" \
  --repo-owner="YOUR_GITHUB_USERNAME" \
  --branch-pattern="^main$" \
  --build-config="db/cloudbuild.yaml" \
  --included-files="db/changelog/**" \
  --substitutions=\
_CLOUD_SQL_INSTANCE="YOUR_PROJECT_ID:europe-central2:mkrew-postgres-europe-central2",\
_DB_NAME="mkrew",\
_DB_USER="mkrew"
```

## Wdrażanie migracji

### Metoda 1: Skrypt deployment (Zalecana)

```bash
cd db

# Nadanie uprawnień do wykonywania
chmod +x scripts/deploy-migrations.sh

# Wdrożenie do dev
./scripts/deploy-migrations.sh dev

# Wdrożenie do staging (wymaga potwierdzenia)
./scripts/deploy-migrations.sh staging

# Wdrożenie do produkcji (wymaga potwierdzenia)
./scripts/deploy-migrations.sh prod
```

**Zmienne środowiskowe:**

```bash
export GCP_PROJECT_ID="your-project-id"
export GCP_REGION="europe-central2"
```

### Metoda 2: Ręczne uruchomienie Cloud Build

```bash
gcloud builds submit \
  --config=db/cloudbuild.yaml \
  --project=YOUR_PROJECT_ID \
  --substitutions=\
_CLOUD_SQL_INSTANCE="YOUR_PROJECT_ID:europe-central2:mkrew-postgres-europe-central2",\
_DB_NAME="mkrew",\
_DB_USER="mkrew"
```

### Metoda 3: Lokalne wykonanie (przez Cloud SQL Proxy)

```bash
# 1. Pobierz Cloud SQL Proxy
wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud_sql_proxy
chmod +x cloud_sql_proxy

# 2. Uruchom proxy
./cloud_sql_proxy -instances=YOUR_PROJECT_ID:europe-central2:mkrew-postgres-europe-central2=tcp:5432 &

# 3. Pobierz hasło z Secret Manager
DB_PASSWORD=$(gcloud secrets versions access latest --secret=mkrew-db-password)

# 4. Uruchom migracje lokalnie
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  update

# 5. Zatrzymaj proxy
pkill -f cloud_sql_proxy
```

## Rollback

### Automatyczny rollback

```bash
cd db

# Nadanie uprawnień
chmod +x scripts/rollback-migration.sh

# Cofnij ostatnią migrację
./scripts/rollback-migration.sh 1 dev

# Cofnij 3 ostatnie migracje w staging
./scripts/rollback-migration.sh 3 staging
```

### Ręczny rollback

```bash
# Uruchom Cloud SQL Proxy (jak wyżej)

# Rollback Count
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  rollbackCount 1

# Rollback do tagu
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  rollback v1.0.0
```

## Monitoring

### Sprawdzanie statusu migracji

```bash
# Historia migracji
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  history

# Status (pending changesets)
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  status
```

### Logi Cloud Build

```bash
# Lista ostatnich buildów
gcloud builds list --limit=10

# Szczegóły konkretnego builda
gcloud builds describe BUILD_ID

# Logi builda
gcloud builds log BUILD_ID
```

### Monitoring Cloud SQL

```bash
# Status instancji
gcloud sql instances describe mkrew-postgres-europe-central2

# Operacje
gcloud sql operations list --instance=mkrew-postgres-europe-central2

# Backupy
gcloud sql backups list --instance=mkrew-postgres-europe-central2
```

## Środowiska

### Dev
- **Project:** `your-project-id-dev`
- **Instance:** `mkrew-postgres-europe-central2`
- **Region:** `europe-central2`
- **Tier:** `db-f1-micro`

### Staging
- **Project:** `your-project-id-staging`
- **Instance:** `mkrew-postgres-staging-europe-central2`
- **Region:** `europe-central2`
- **Tier:** `db-g1-small`

### Production
- **Project:** `your-project-id-prod`
- **Instance:** `mkrew-postgres-prod-europe-central2`
- **Region:** `europe-central2`
- **Tier:** `db-custom-2-7680` (2 vCPU, 7.5 GB RAM)
- **Availability:** REGIONAL (High Availability)

## Bezpieczeństwo

### Best Practices

1. **Hasła:**
   - Używaj Secret Manager dla haseł
   - Nigdy nie commituj haseł do repozytorium
   - Rotuj hasła co 90 dni

2. **Uprawnienia:**
   - Principle of least privilege
   - Service accounts dla automatyzacji
   - Audyt dostępu co kwartał

3. **Backup:**
   - Automatyczne backupy włączone
   - Point-in-Time Recovery włączony
   - Testy restore co miesiąc

4. **SSL/TLS:**
   - Wymagaj SSL dla połączeń
   - Użyj Cloud SQL Proxy w produkcji

5. **Network:**
   - Private IP dla produkcji (VPC)
   - Authorized networks dla dev/staging

## Troubleshooting

### Problem: "Permission denied"

```bash
# Sprawdź uprawnienia
gcloud projects get-iam-policy YOUR_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:YOUR_PROJECT_ID@cloudbuild.gserviceaccount.com"

# Dodaj brakujące uprawnienia
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:YOUR_PROJECT_ID@cloudbuild.gserviceaccount.com" \
  --role="roles/cloudsql.client"
```

### Problem: "Could not connect to Cloud SQL"

```bash
# Sprawdź czy instancja działa
gcloud sql instances describe mkrew-postgres-europe-central2 --format="value(state)"

# Restart instancji
gcloud sql instances restart mkrew-postgres-europe-central2

# Sprawdź logi
gcloud sql instances logs list mkrew-postgres-europe-central2
```

### Problem: "Liquibase lock"

```sql
-- Połącz się z bazą i usuń lock
DELETE FROM DATABASECHANGELOGLOCK WHERE ID=1;
```

### Problem: "Changeset already executed"

```bash
# Sprawdź historię
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  history

# Opcja 1: Rollback i ponowne wykonanie
# Opcja 2: Oznacz jako wykonany bez faktycznego wykonania
# clearCheckSums (tylko jeśli wiesz co robisz!)
```

## Koszty

### Szacunkowe koszty miesięczne (europe-central2)

| Tier | vCPU | RAM | Storage | Koszt/msc |
|------|------|-----|---------|-----------|
| db-f1-micro | Shared | 0.6 GB | 10 GB | ~$7 |
| db-g1-small | Shared | 1.7 GB | 10 GB | ~$25 |
| db-custom-2-7680 | 2 | 7.5 GB | 10 GB | ~$120 |

**Dodatkowe koszty:**
- Backupy: ~$0.08/GB/miesiąc
- Network egress: ~$0.12/GB
- Cloud Build: Pierwsze 120 min/dzień gratis

## Przydatne komendy

```bash
# Status wszystkich instancji
gcloud sql instances list

# Połączenie przez gcloud
gcloud sql connect mkrew-postgres-europe-central2 --user=mkrew --database=mkrew

# Export bazy
gcloud sql export sql mkrew-postgres-europe-central2 gs://YOUR_BUCKET/backup.sql \
  --database=mkrew

# Import bazy
gcloud sql import sql mkrew-postgres-europe-central2 gs://YOUR_BUCKET/backup.sql \
  --database=mkrew

# Zmiana tier
gcloud sql instances patch mkrew-postgres-europe-central2 \
  --tier=db-custom-2-7680

# Point-in-Time Recovery
gcloud sql backups create \
  --instance=mkrew-postgres-europe-central2 \
  --description="Manual backup before major migration"
```

## Referencje

- [Cloud SQL Documentation](https://cloud.google.com/sql/docs)
- [Liquibase Documentation](https://docs.liquibase.com)
- [Cloud Build Documentation](https://cloud.google.com/build/docs)
- [Terraform Google Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs)

---

**Ostatnia aktualizacja:** 2025-01-07
**Wersja:** 1.0
