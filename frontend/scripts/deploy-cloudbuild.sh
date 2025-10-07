#!/bin/bash
# Skrypt do wdrożenia frontendu przez Cloud Build

set -e

# Kolory dla output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Konfiguracja
PROJECT_ID=${GCP_PROJECT_ID:-"613243186971"}
REGION=${GCP_REGION:-"europe-central2"}
CLUSTER_NAME=${GKE_CLUSTER:-"mkrew-cluster"}

echo -e "${BLUE}🚀 Deploy Frontend to GKE via Cloud Build${NC}"
echo ""

# Sprawdź czy jesteś zalogowany do GCP
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &>/dev/null; then
    echo -e "${RED}❌ Nie jesteś zalogowany do GCP!${NC}"
    echo "Uruchom: gcloud auth login"
    exit 1
fi

# Ustaw projekt
echo -e "${BLUE}📦 Ustawianie projektu: $PROJECT_ID${NC}"
gcloud config set project $PROJECT_ID

# Sprawdź czy klaster istnieje
echo -e "${BLUE}🔍 Sprawdzanie klastra GKE...${NC}"
if ! gcloud container clusters describe $CLUSTER_NAME --region=$REGION &>/dev/null; then
    echo -e "${RED}❌ Klaster $CLUSTER_NAME nie istnieje w regionie $REGION${NC}"
    echo "Utwórz klaster najpierw lub zmień CLUSTER_NAME"
    exit 1
fi

echo -e "${GREEN}✅ Klaster znaleziony${NC}"

# Zbuduj i wdróż
echo -e "${BLUE}🏗️  Budowanie i wdrażanie przez Cloud Build...${NC}"
cd "$(dirname "$0")/../.."

gcloud builds submit \
    --config=frontend/cloudbuild.yaml \
    --region=$REGION \
    --project=$PROJECT_ID

echo ""
echo -e "${GREEN}✅ Deployment zakończony!${NC}"
echo ""
echo -e "${BLUE}Sprawdź status:${NC}"
echo "  kubectl get pods -l app=mkrew-frontend"
echo "  kubectl get ingress mkrew-frontend-ingress"
echo ""
echo -e "${BLUE}Logi Cloud Build:${NC}"
echo "  https://console.cloud.google.com/cloud-build/builds?project=$PROJECT_ID"
