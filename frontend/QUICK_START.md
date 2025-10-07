# Quick Start - Deployment Frontend GKE

## 🚀 Wybierz metodę wdrożenia

### Opcja A: Cloud Build (Zalecane dla GCP)
### Opcja B: GitHub Actions (Zalecane dla multi-cloud)

---

## 📦 Opcja A: Cloud Build

### Szybki start (5 minut):

```bash
# 1. Ustaw projekt
export PROJECT_ID="613243186971"
gcloud config set project $PROJECT_ID

# 2. Włącz API i nadaj uprawnienia
gcloud services enable cloudbuild.googleapis.com container.googleapis.com

PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/container.developer"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/storage.admin"

# 3. Połącz GitHub (w konsoli)
# Przejdź do: https://console.cloud.google.com/cloud-build/triggers
# Kliknij "Connect Repository" → GitHub → Wybierz mkrew2

# 4. Utwórz trigger
gcloud builds triggers create github \
  --name="frontend-deploy" \
  --repo-name="mkrew2" \
  --repo-owner="YOUR_GITHUB_USERNAME" \
  --branch-pattern="^main$" \
  --build-config="frontend/cloudbuild.yaml"

# 5. Push = auto deploy!
git push origin main
```

**Sprawdź:** https://console.cloud.google.com/cloud-build/builds

---

## 🐙 Opcja B: GitHub Actions

### Szybki start (10 minut):

```bash
# 1. Utwórz Service Account
export PROJECT_ID="613243186971"

gcloud iam service-accounts create github-actions \
  --project="${PROJECT_ID}" \
  --display-name="GitHub Actions"

# 2. Nadaj uprawnienia
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/container.developer"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# 3. Wygeneruj klucz JSON
gcloud iam service-accounts keys create github-key.json \
  --iam-account=github-actions@${PROJECT_ID}.iam.gserviceaccount.com

# 4. Dodaj GitHub Secrets (w repo na GitHub):
# Settings → Secrets and variables → Actions → New secret

# GCP_PROJECT_ID = 613243186971
# GCP_SA_KEY = (wklej zawartość github-key.json)

# 5. Zaktualizuj workflow (.github/workflows/frontend-deploy-gke.yml)
# Zamień Workload Identity na:
```

```yaml
- name: Authenticate to Google Cloud
  uses: google-github-actions/auth@v2
  with:
    credentials_json: ${{ secrets.GCP_SA_KEY }}
```

```bash
# 6. Push = auto deploy!
git push origin main
```

**Sprawdź:** GitHub → Actions tab

---

## 🛠️ Wdrożenie manualne (bez CI/CD)

### Build i deploy ręcznie:

```bash
# 1. Pobierz credentials GKE
gcloud container clusters get-credentials mkrew-cluster \
  --region=europe-central2 \
  --project=613243186971

# 2. Build i push Docker image
cd frontend
docker build -t gcr.io/613243186971/mkrew-frontend:v1.0.0 .

gcloud auth configure-docker gcr.io
docker push gcr.io/613243186971/mkrew-frontend:v1.0.0

# 3. Zaktualizuj deployment.yaml
sed -i "s/PROJECT_ID/613243186971/g" k8s/deployment.yaml

# 4. Deploy do GKE
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml

# 5. Sprawdź status
kubectl get pods -l app=mkrew-frontend
kubectl get ingress mkrew-frontend-ingress
```

---

## 🌐 Konfiguracja domeny

### 1. Pobierz IP Ingress:

```bash
kubectl get ingress mkrew-frontend-ingress

# EXTERNAL-IP: 34.117.XXX.XXX (poczekaj 5-10 min)
```

### 2. Skonfiguruj DNS:

W swoim dostawcy domeny (mkrew.pl):
```
Type: A
Name: @ (lub www)
Value: 34.117.XXX.XXX
TTL: 300
```

### 3. SSL (automatyczne):

```bash
# Sprawdź status certyfikatu (może potrwać 15-60 min)
kubectl describe managedcertificate mkrew-frontend-cert

# Status: Active → SSL gotowe!
```

### 4. Test:

```bash
curl https://mkrew.pl
```

---

## 📊 Monitoring

### Podstawowe komendy:

```bash
# Logi
kubectl logs -f -l app=mkrew-frontend

# Status podów
kubectl get pods -l app=mkrew-frontend

# HPA (autoscaling)
kubectl get hpa mkrew-frontend-hpa

# Ingress
kubectl get ingress

# Events
kubectl get events --sort-by='.lastTimestamp' | grep mkrew-frontend
```

### Cloud Console:

- **GKE:** https://console.cloud.google.com/kubernetes/workload
- **Logs:** https://console.cloud.google.com/logs/query
- **Metrics:** https://console.cloud.google.com/monitoring

---

## 🔄 Update aplikacji

### Przez CI/CD (auto):
```bash
git add frontend/
git commit -m "Update UI"
git push origin main
```

### Manualnie:
```bash
# Build nową wersję
docker build -t gcr.io/613243186971/mkrew-frontend:v1.1.0 ./frontend
docker push gcr.io/613243186971/mkrew-frontend:v1.1.0

# Rolling update
kubectl set image deployment/mkrew-frontend \
  frontend=gcr.io/613243186971/mkrew-frontend:v1.1.0

# Sprawdź rollout
kubectl rollout status deployment/mkrew-frontend
```

---

## 🐛 Troubleshooting

### Problem: Pody nie startują

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Problem: Ingress bez IP

```bash
# Sprawdź backend health
kubectl describe ingress mkrew-frontend-ingress

# GCP Console → Network Services → Load Balancing
```

### Problem: SSL nie działa

```bash
# Status cert
kubectl describe managedcertificate mkrew-frontend-cert

# DNS propagation
nslookup mkrew.pl
```

### Rollback:

```bash
# Do poprzedniej wersji
kubectl rollout undo deployment/mkrew-frontend

# Do konkretnej rewizji
kubectl rollout history deployment/mkrew-frontend
kubectl rollout undo deployment/mkrew-frontend --to-revision=2
```

---

## 📚 Pełna dokumentacja

- **Deployment Guide:** `DEPLOYMENT.md`
- **Cloud Build Setup:** `CLOUDBUILD_SETUP.md`
- **Project Structure:** `../PROJECT_STRUCTURE.md`

---

## ✅ Checklist wdrożenia

**Przed pierwszym deployem:**

- [ ] Klaster GKE utworzony
- [ ] API włączone (Cloud Build / Container Registry)
- [ ] Uprawnienia dla SA skonfigurowane
- [ ] CI/CD setup (Cloud Build trigger LUB GitHub Secrets)
- [ ] Domena skierowana na Ingress IP
- [ ] SSL certificate Status: Active

**Po deploymencie:**

- [ ] Pody działają (`kubectl get pods`)
- [ ] Ingress ma IP publiczny
- [ ] Domena resolves do IP
- [ ] SSL działa (https://)
- [ ] Aplikacja odpowiada (200 OK)
- [ ] Monitoring skonfigurowany

---

**Pytania? Zobacz:** `DEPLOYMENT.md` lub `CLOUDBUILD_SETUP.md`
