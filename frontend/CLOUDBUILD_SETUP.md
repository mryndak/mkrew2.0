# Cloud Build Setup - Frontend Deployment

## 🎯 Co to Cloud Build?

Cloud Build to usługa CI/CD od Google Cloud, która:
- **Builduje** Docker images bezpośrednio w GCP
- **Wdraża** aplikacje na GKE automatycznie
- **Integruje się** z GitHub (trigger na push)
- **Nie wymaga** GitHub Actions ani zewnętrznych runnerów

---

## 🚀 Setup krok po kroku

### 1. Włącz Cloud Build API

```bash
export PROJECT_ID="613243186971"

gcloud config set project $PROJECT_ID

gcloud services enable cloudbuild.googleapis.com
gcloud services enable container.googleapis.com
gcloud services enable containerregistry.googleapis.com
```

### 2. Nadaj uprawnienia dla Cloud Build

```bash
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")

# Uprawnienia do GKE
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/container.developer"

# Uprawnienia do GCR
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/storage.admin"

# Service Account User
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

# Kubernetes Engine Admin (opcjonalnie, dla większych uprawnień)
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/container.admin"
```

### 3. Połącz GitHub z Cloud Build

#### Opcja A: Przez konsolę GCP (łatwiejsze)

1. **Przejdź do Cloud Build:**
   ```
   https://console.cloud.google.com/cloud-build/triggers?project=613243186971
   ```

2. **Kliknij "Connect Repository"**
   - Wybierz **GitHub (Cloud Build GitHub App)**
   - Autoryzuj GCP w GitHub
   - Wybierz repozytorium: `mkrew2`
   - Kliknij "Connect"

3. **Utwórz Trigger:**
   - Kliknij **"Create Trigger"**
   - Wypełnij formularz:

```yaml
Name: frontend-deploy
Description: Deploy frontend to GKE on push to main
Event: Push to a branch
Source:
  Repository: YOUR_USERNAME/mkrew2 (poprzednio połączone)
  Branch: ^main$
Filters (opcjonalnie):
  Included files filter: frontend/**
Configuration:
  Type: Cloud Build configuration file (yaml or json)
  Location: frontend/cloudbuild.yaml
Advanced:
  Service account: Leave default or select custom
  Substitution variables: (opcjonalnie)
    _CLUSTER_NAME: mkrew-cluster
    _REGION: europe-central2
```

4. **Kliknij "Create"**

#### Opcja B: Przez CLI

```bash
export GITHUB_OWNER="YOUR_GITHUB_USERNAME"  # ZMIEŃ!
export GITHUB_REPO="mkrew2"

# Najpierw połącz repo (tylko raz)
gcloud builds repositories create \
  --remote-uri="https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}.git" \
  --connection="YOUR_CONNECTION_NAME" \
  ${GITHUB_REPO}

# Utwórz trigger
gcloud builds triggers create github \
  --name="frontend-deploy" \
  --repo-name="${GITHUB_REPO}" \
  --repo-owner="${GITHUB_OWNER}" \
  --branch-pattern="^main$" \
  --build-config="frontend/cloudbuild.yaml" \
  --region="europe-central2" \
  --description="Deploy frontend to GKE"
```

---

## 📝 Jak działa cloudbuild.yaml

Nasz plik `frontend/cloudbuild.yaml` wykonuje:

```yaml
1. Build Docker image       → gcr.io/PROJECT_ID/mkrew-frontend:SHORT_SHA
2. Push to GCR             → latest + SHA tags
3. Replace PROJECT_ID      → w deployment.yaml
4. Apply ConfigMap         → kubectl apply
5. Apply Deployment        → kubectl apply
6. Apply HPA               → kubectl apply
7. Apply Ingress           → kubectl apply
8. Update image            → rolling update
9. Wait for rollout        → verify deployment
```

---

## 🔄 Użycie

### Automatyczne (Trigger GitHub)

Po skonfigurowaniu triggera:

```bash
# Każdy push do main automatycznie wdraża
git add .
git commit -m "Update frontend"
git push origin main
```

**Sprawdź build:**
```
https://console.cloud.google.com/cloud-build/builds?project=613243186971
```

### Manualne (bez GitHub)

```bash
# Z katalogu głównego projektu
gcloud builds submit \
  --config=frontend/cloudbuild.yaml \
  --region=europe-central2 \
  --project=613243186971

# Z custom tagiem
gcloud builds submit \
  --config=frontend/cloudbuild.yaml \
  --region=europe-central2 \
  --substitutions=_IMAGE_TAG=v2.0.0 \
  --project=613243186971
```

### Używając skryptu pomocniczego

```bash
# Nadaj uprawnienia
chmod +x frontend/scripts/deploy-cloudbuild.sh

# Uruchom
./frontend/scripts/deploy-cloudbuild.sh
```

---

## 🔍 Monitorowanie

### Logi build

```bash
# Ostatnie buildy
gcloud builds list --limit=5

# Szczegóły konkretnego builda
gcloud builds log BUILD_ID

# Stream logów live
gcloud builds log BUILD_ID --stream
```

### Cloud Console

1. **Builds History:**
   ```
   https://console.cloud.google.com/cloud-build/builds
   ```

2. **Triggers:**
   ```
   https://console.cloud.google.com/cloud-build/triggers
   ```

3. **Container Registry (images):**
   ```
   https://console.cloud.google.com/gcr/images/613243186971
   ```

---

## ⚙️ Konfiguracja zaawansowana

### Dodatkowe zmienne (substitutions)

Edytuj trigger lub dodaj do `cloudbuild.yaml`:

```yaml
substitutions:
  _CLUSTER_NAME: mkrew-cluster
  _REGION: europe-central2
  _NAMESPACE: default

# Użyj w steps:
env:
  - 'CLOUDSDK_CONTAINER_CLUSTER=${_CLUSTER_NAME}'
  - 'CLOUDSDK_COMPUTE_REGION=${_REGION}'
```

### Build tylko przy zmianach w frontend/

W triggerze dodaj filter:

```yaml
# Included files filter
frontend/**

# Ignored files filter
**.md
docs/**
```

Lub w CLI:

```bash
gcloud builds triggers update frontend-deploy \
  --included-files="frontend/**"
```

### Notyfikacje (Slack, Email)

```bash
# Pub/Sub dla notyfikacji
gcloud builds triggers update frontend-deploy \
  --subscription="projects/613243186971/subscriptions/build-notifications"
```

---

## 🆚 Cloud Build vs GitHub Actions

| Feature | Cloud Build | GitHub Actions |
|---------|-------------|----------------|
| **Hosting** | GCP | GitHub |
| **Koszt** | Pierwsze 120 min/dzień FREE | 2000 min/miesiąc FREE (public) |
| **Szybkość** | Szybsze (w GCP) | Wolniejsze (transfer) |
| **Integracja GCP** | Natywna | Wymaga auth |
| **Secrets** | GCP Secret Manager | GitHub Secrets |
| **Cache** | GCR | Docker layers |

**Zalecenie:**
- **Cloud Build:** Jeśli cały stack w GCP
- **GitHub Actions:** Jeśli multi-cloud lub GitOps workflow

---

## 🐛 Troubleshooting

### ❌ Permission denied (kubectl)

```bash
# Sprawdź uprawnienia
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

# Dodaj brakujące
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/container.developer"
```

### ❌ Cluster not found

Sprawdź w `cloudbuild.yaml`:
```yaml
env:
  - 'CLOUDSDK_COMPUTE_REGION=europe-central2'  # Musi być REGION!
  - 'CLOUDSDK_CONTAINER_CLUSTER=mkrew-cluster'
```

Zonal cluster używa `CLOUDSDK_COMPUTE_ZONE`, regional używa `CLOUDSDK_COMPUTE_REGION`.

### ❌ Image pull error

```bash
# Sprawdź czy obraz istnieje
gcloud container images list --repository=gcr.io/$PROJECT_ID

# Sprawdź uprawnienia
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/storage.admin"
```

### ❌ Build timeout

W `cloudbuild.yaml` zwiększ timeout:
```yaml
timeout: '1800s'  # 30 minut
```

---

## 💡 Tips & Best Practices

### 1. **Używaj cache dla npm**

```yaml
steps:
  - name: 'node:18-alpine'
    entrypoint: npm
    args: ['ci', '--cache', '.npm']
    dir: 'frontend'
```

### 2. **Parallel builds (szybsze)**

```yaml
steps:
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'image1', '.']
    waitFor: ['-']  # Start immediately

  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'image2', '.']
    waitFor: ['-']  # Start immediately
```

### 3. **Multi-stage testing**

```yaml
steps:
  - name: 'node:18'
    args: ['npm', 'test']
    dir: 'frontend'

  - name: 'node:18'
    args: ['npm', 'run', 'lint']
    dir: 'frontend'
```

### 4. **Secret management**

```bash
# Utwórz secret
echo -n "my-secret-value" | gcloud secrets create my-secret --data-file=-

# Użyj w build
steps:
  - name: 'gcr.io/cloud-builders/gcloud'
    entrypoint: 'bash'
    args:
      - '-c'
      - |
        gcloud secrets versions access latest --secret=my-secret > /workspace/secret.txt
```

---

## 📚 Resources

- **Cloud Build Docs:** https://cloud.google.com/build/docs
- **Builder Images:** https://cloud.google.com/build/docs/cloud-builders
- **Pricing:** https://cloud.google.com/build/pricing
- **GitHub App:** https://github.com/apps/google-cloud-build

---

**Ostatnia aktualizacja:** 2025-10-04
