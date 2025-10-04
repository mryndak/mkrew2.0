# Wdrożenie Frontend na GKE (Google Kubernetes Engine)

## 📋 Spis treści
1. [Wymagania wstępne](#wymagania-wstępne)
2. [Konfiguracja GCP](#konfiguracja-gcp)
3. [Utworzenie klastra GKE](#utworzenie-klastra-gke)
4. [Konfiguracja CI/CD](#konfiguracja-cicd)
5. [Wdrożenie manualne](#wdrożenie-manualne)
6. [Konfiguracja domeny i SSL](#konfiguracja-domeny-i-ssl)
7. [Monitoring i skalowanie](#monitoring-i-skalowanie)
8. [Troubleshooting](#troubleshooting)

## 🔧 Wymagania wstępne

### Narzędzia lokalne:
```bash
# Google Cloud SDK
gcloud version

# kubectl
kubectl version --client

# Docker
docker --version
```

### Instalacja narzędzi (jeśli nie masz):
```bash
# Google Cloud SDK (Windows)
# Pobierz: https://cloud.google.com/sdk/docs/install

# kubectl
gcloud components install kubectl

# Opcjonalnie: Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

## ☁️ Konfiguracja GCP

### 1. Utwórz projekt GCP
```bash
# Zaloguj się
gcloud auth login

# Utwórz nowy projekt (lub użyj istniejącego)
export PROJECT_ID="mkrew-production"
gcloud projects create $PROJECT_ID --name="MKrew Blood Forecasting"

# Ustaw jako domyślny projekt
gcloud config set project $PROJECT_ID

# Włącz billing (wymagane dla GKE)
# https://console.cloud.google.com/billing
```

### 2. Włącz wymagane API
```bash
gcloud services enable \
  container.googleapis.com \
  containerregistry.googleapis.com \
  compute.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com
```

### 3. Utwórz Service Account dla CI/CD
```bash
# Service Account dla GitHub Actions
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions Deployer"

# Nadaj uprawnienia
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/container.developer"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

### 4. Konfiguracja Workload Identity Federation (zalecane)
```bash
# Utwórz Workload Identity Pool
gcloud iam workload-identity-pools create "github-pool" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --display-name="GitHub Actions Pool"

# Utwórz Workload Identity Provider
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --display-name="GitHub Provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository" \
  --issuer-uri="https://token.actions.githubusercontent.com"

# Pobierz Workload Identity Provider name
gcloud iam workload-identity-pools providers describe "github-provider" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --format="value(name)"
```

## 🚀 Utworzenie klastra GKE

### Opcja 1: Standard GKE Cluster (zalecane dla production)
```bash
export CLUSTER_NAME="mkrew-cluster"
export REGION="europe-central2"  # Warszawa

gcloud container clusters create $CLUSTER_NAME \
  --region=$REGION \
  --num-nodes=1 \
  --machine-type=e2-medium \
  --disk-size=20 \
  --disk-type=pd-standard \
  --enable-autoscaling \
  --min-nodes=1 \
  --max-nodes=5 \
  --enable-autorepair \
  --enable-autoupgrade \
  --network="default" \
  --subnetwork="default" \
  --enable-ip-alias \
  --workload-pool=${PROJECT_ID}.svc.id.goog \
  --addons=HorizontalPodAutoscaling,HttpLoadBalancing,GcePersistentDiskCsiDriver
```

### Opcja 2: GKE Autopilot (łatwiejsze zarządzanie)
```bash
gcloud container clusters create-auto $CLUSTER_NAME \
  --region=$REGION \
  --project=$PROJECT_ID
```

### Pobierz credentials
```bash
gcloud container clusters get-credentials $CLUSTER_NAME \
  --region=$REGION \
  --project=$PROJECT_ID

# Sprawdź połączenie
kubectl get nodes
```

## 🔐 Konfiguracja CI/CD

### Opcja A: GitHub Actions (zalecane)

#### 1. Skonfiguruj GitHub Secrets
Przejdź do: `Settings > Secrets and variables > Actions`

**Wymagane sekrety:**
```
GCP_PROJECT_ID=mkrew-production
WIF_PROVIDER=projects/123456789/locations/global/workloadIdentityPools/github-pool/providers/github-provider
WIF_SERVICE_ACCOUNT=github-actions@mkrew-production.iam.gserviceaccount.com
```

#### 2. Workflow automatycznie uruchomi się przy push do `main`
```bash
git add .
git commit -m "Add GKE deployment configuration"
git push origin main
```

### Opcja B: Google Cloud Build

#### 1. Utwórz trigger w Cloud Build
```bash
gcloud builds triggers create github \
  --name="frontend-deploy" \
  --repo-name="mkrew2" \
  --repo-owner="YOUR_GITHUB_USERNAME" \
  --branch-pattern="^main$" \
  --build-config="frontend/cloudbuild.yaml"
```

#### 2. Nadaj uprawnienia dla Cloud Build
```bash
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/container.developer"
```

## 🛠️ Wdrożenie manualne

### 1. Zbuduj i wypchnij obraz Docker
```bash
# Konfiguruj Docker dla GCR
gcloud auth configure-docker gcr.io

# Zbuduj obraz
cd frontend
docker build -t gcr.io/$PROJECT_ID/mkrew-frontend:v1.0.0 .

# Wypchnij do GCR
docker push gcr.io/$PROJECT_ID/mkrew-frontend:v1.0.0
```

### 2. Zaktualizuj manifesty Kubernetes
```bash
# Zamień PROJECT_ID w deployment.yaml
sed -i "s/PROJECT_ID/$PROJECT_ID/g" k8s/deployment.yaml

# Lub ręcznie edytuj:
# image: gcr.io/mkrew-production/mkrew-frontend:v1.0.0
```

### 3. Zastosuj manifesty
```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml
```

### 4. Sprawdź status
```bash
# Pody
kubectl get pods -l app=mkrew-frontend

# Deployment
kubectl get deployment mkrew-frontend

# Service
kubectl get service mkrew-frontend-service

# Ingress
kubectl get ingress mkrew-frontend-ingress
```

## 🌐 Konfiguracja domeny i SSL

### 1. Pobierz IP adres Ingress
```bash
kubectl get ingress mkrew-frontend-ingress

# Poczekaj aż EXTERNAL-IP się pojawi (może potrwać 5-10 minut)
# NAME                      CLASS    HOSTS                ADDRESS          PORTS   AGE
# mkrew-frontend-ingress    <none>   mkrew.example.com    34.117.XXX.XXX   80      5m
```

### 2. Skonfiguruj DNS
W swoim dostawcy domeny (np. CloudFlare, GoDaddy):
```
Type: A
Name: mkrew (lub @)
Value: 34.117.XXX.XXX (IP z poprzedniego kroku)
TTL: Auto lub 300
```

### 3. Zaktualizuj Ingress z prawdziwą domeną
```bash
# Edytuj k8s/ingress.yaml
# Zmień: mkrew.example.com -> Twoja domena (np. mkrew.pl)

kubectl apply -f k8s/ingress.yaml
```

### 4. SSL Certificate (Google-managed)
```bash
# Sprawdź status certyfikatu
kubectl describe managedcertificate mkrew-frontend-cert

# Status: Active (może potrwać 15-60 minut)
```

### 5. Wymuś HTTPS (opcjonalnie)
```yaml
# Dodaj do ingress.yaml annotations:
annotations:
  kubernetes.io/ingress.global-static-ip-name: "mkrew-frontend-ip"
  networking.gke.io/v1beta1.FrontendConfig: "ssl-redirect"
```

## 📊 Monitoring i skalowanie

### Monitoring
```bash
# Logi podów
kubectl logs -f -l app=mkrew-frontend

# Metryki
kubectl top pods -l app=mkrew-frontend
kubectl top nodes

# Events
kubectl get events --sort-by='.lastTimestamp'
```

### Cloud Console
1. **GKE Dashboard:** https://console.cloud.google.com/kubernetes
2. **Cloud Logging:** https://console.cloud.google.com/logs
3. **Cloud Monitoring:** https://console.cloud.google.com/monitoring

### Horizontal Pod Autoscaling (HPA)
```bash
# Sprawdź HPA
kubectl get hpa mkrew-frontend-hpa

# Szczegóły
kubectl describe hpa mkrew-frontend-hpa

# HPA automatycznie skaluje 2-10 podów przy 70% CPU
```

### Testowanie skalowania
```bash
# Stress test (opcjonalnie)
kubectl run -i --tty load-generator --rm --image=busybox --restart=Never -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://mkrew-frontend-service; done"

# Obserwuj skalowanie
watch kubectl get hpa,pods
```

## 🔧 Troubleshooting

### Problem: Pody nie startują
```bash
# Sprawdź events
kubectl describe pod <pod-name>

# Logi
kubectl logs <pod-name>

# Sprawdź image pull
kubectl get events | grep -i "image"
```

### Problem: Ingress nie ma IP
```bash
# Sprawdź backend health
kubectl get ingress mkrew-frontend-ingress -o yaml

# Cloud Console > Network Services > Load Balancing
# Sprawdź health checks
```

### Problem: SSL nie działa
```bash
# Status certyfikatu
kubectl describe managedcertificate mkrew-frontend-cert

# Sprawdź DNS propagation
nslookup mkrew.example.com
dig mkrew.example.com

# Google wymaga, aby domena była dostępna przed wydaniem SSL
```

### Problem: 502 Bad Gateway
```bash
# Sprawdź readiness probe
kubectl get pods -o wide

# Test lokalnie
kubectl port-forward deployment/mkrew-frontend 8080:4321
curl http://localhost:8080
```

### Rollback deploymentu
```bash
# Historia
kubectl rollout history deployment/mkrew-frontend

# Rollback do poprzedniej wersji
kubectl rollout undo deployment/mkrew-frontend

# Rollback do konkretnej rewizji
kubectl rollout undo deployment/mkrew-frontend --to-revision=2
```

## 🔄 Aktualizacja aplikacji

### Przez GitHub Actions (automatyczne)
```bash
git add frontend/
git commit -m "Update frontend"
git push origin main
# GitHub Actions automatycznie wdroży
```

### Manualnie
```bash
# Build nowego obrazu
docker build -t gcr.io/$PROJECT_ID/mkrew-frontend:v1.0.1 ./frontend
docker push gcr.io/$PROJECT_ID/mkrew-frontend:v1.0.1

# Update deployment
kubectl set image deployment/mkrew-frontend \
  frontend=gcr.io/$PROJECT_ID/mkrew-frontend:v1.0.1

# Obserwuj rollout
kubectl rollout status deployment/mkrew-frontend
```

## 💰 Koszty i optymalizacja

### Szacunkowe koszty miesięczne (eu-central2):
- **GKE Autopilot:** ~$70-150/miesiąc (płacisz za zużyte zasoby)
- **GKE Standard:** ~$30-80/miesiąc (1-3 nody e2-medium)
- **Load Balancer:** ~$18/miesiąc
- **Egress:** ~$0.12/GB (pierwsze 1GB free)

### Optymalizacja kosztów:
```bash
# Użyj Spot VMs (do 80% taniej)
gcloud container node-pools create spot-pool \
  --cluster=$CLUSTER_NAME \
  --region=$REGION \
  --spot \
  --enable-autoscaling \
  --min-nodes=1 \
  --max-nodes=3

# Skonfiguruj cluster autoscaler
kubectl scale deployment mkrew-frontend --replicas=1  # poza godzinami szczytu
```

## 🧹 Czyszczenie zasobów

### Usuń deployment (zachowaj klaster)
```bash
kubectl delete -f frontend/k8s/
```

### Usuń cały klaster
```bash
gcloud container clusters delete $CLUSTER_NAME \
  --region=$REGION \
  --project=$PROJECT_ID
```

### Usuń obrazy Docker
```bash
gcloud container images delete gcr.io/$PROJECT_ID/mkrew-frontend:latest
```

## 📚 Przydatne komendy

```bash
# Kontekst kubectl
kubectl config get-contexts
kubectl config use-context gke_${PROJECT_ID}_${REGION}_${CLUSTER_NAME}

# Namespace
kubectl get namespaces
kubectl config set-context --current --namespace=default

# Shell do poda
kubectl exec -it <pod-name> -- /bin/sh

# Port forwarding
kubectl port-forward service/mkrew-frontend-service 8080:80

# Kopiowanie plików
kubectl cp <pod-name>:/app/dist ./local-dist

# Restart deployment
kubectl rollout restart deployment/mkrew-frontend
```

## 🔗 Przydatne linki

- **GKE Documentation:** https://cloud.google.com/kubernetes-engine/docs
- **Kubernetes Docs:** https://kubernetes.io/docs/
- **GCP Console:** https://console.cloud.google.com
- **Cloud Build:** https://cloud.google.com/build/docs
- **Workload Identity:** https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity

---

**Ostatnia aktualizacja:** 2025-10-04
**Autor:** Claude Code
