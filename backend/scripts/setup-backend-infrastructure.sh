#!/bin/bash
#
# Setup infrastructure for Backend service
# Usage: ./setup-backend-infrastructure.sh [project_id] [environment]
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ID="${1:-your-project-id}"
ENVIRONMENT="${2:-dev}"
REGION="${GCP_REGION:-europe-central2}"

echo -e "${GREEN}=== Setting up Backend Infrastructure ===${NC}"
echo -e "${YELLOW}Project ID:${NC} ${PROJECT_ID}"
echo -e "${YELLOW}Environment:${NC} ${ENVIRONMENT}"
echo -e "${YELLOW}Region:${NC} ${REGION}"

# Confirm
read -p "Continue with setup? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo -e "${RED}Setup cancelled${NC}"
  exit 0
fi

gcloud config set project "${PROJECT_ID}"

echo -e "${BLUE}Step 1: Enabling APIs...${NC}"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  sqladmin.googleapis.com \
  sql-component.googleapis.com \
  containerregistry.googleapis.com

echo -e "${GREEN}✓ APIs enabled${NC}"

echo -e "${BLUE}Step 2: Creating Backend service account...${NC}"

if gcloud iam service-accounts describe "mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" &>/dev/null; then
  echo "Backend service account already exists"
else
  gcloud iam service-accounts create mkrew-backend-sa \
    --display-name="Backend Service Account" \
    --description="Service account for Backend API"
  echo -e "${GREEN}✓ Backend service account created${NC}"
fi

echo -e "${BLUE}Step 3: Generating secrets...${NC}"

# JWT Secret
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
echo -n "$JWT_SECRET" | gcloud secrets create mkrew-jwt-secret \
  --data-file=- \
  --replication-policy=automatic 2>/dev/null \
  || echo -n "$JWT_SECRET" | gcloud secrets versions add mkrew-jwt-secret --data-file=-

echo -e "${GREEN}✓ JWT secret created${NC}"

# Scraper API Key
SCRAPER_API_KEY=$(openssl rand -base64 48 | tr -d '\n')
echo -n "$SCRAPER_API_KEY" | gcloud secrets create mkrew-scraper-api-key \
  --data-file=- \
  --replication-policy=automatic 2>/dev/null \
  || echo -n "$SCRAPER_API_KEY" | gcloud secrets versions add mkrew-scraper-api-key --data-file=-

echo -e "${GREEN}✓ Scraper API key created${NC}"

echo ""
echo -e "${YELLOW}=== Generated Secrets (save for local development) ===${NC}"
echo -e "${YELLOW}JWT Secret:${NC}"
echo "$JWT_SECRET"
echo ""
echo -e "${YELLOW}Scraper API Key:${NC}"
echo "$SCRAPER_API_KEY"
echo ""

echo -e "${BLUE}Step 4: Configuring IAM permissions...${NC}"

# Backend can read its secrets
gcloud secrets add-iam-policy-binding mkrew-jwt-secret \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding mkrew-scraper-api-key \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Backend can read ML API key
gcloud secrets add-iam-policy-binding mkrew-ml-api-key \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" 2>/dev/null \
  || echo "ML API key not found (deploy ML service first)"

# Backend can read DB password
gcloud secrets add-iam-policy-binding mkrew-db-password \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" 2>/dev/null \
  || echo "DB password not found (deploy database first)"

# Backend can connect to Cloud SQL
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

# Cloud Build permissions
gcloud secrets add-iam-policy-binding mkrew-jwt-secret \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding mkrew-scraper-api-key \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/cloudsql.client"

gcloud iam service-accounts add-iam-policy-binding \
  "mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

echo -e "${GREEN}✓ IAM permissions configured${NC}"

echo ""
echo -e "${GREEN}=== Backend Infrastructure Setup Complete ===${NC}"
echo ""
echo -e "${YELLOW}Service Account:${NC}"
echo "  mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com"
echo ""
echo -e "${YELLOW}Secrets in Secret Manager:${NC}"
echo "  - mkrew-jwt-secret"
echo "  - mkrew-scraper-api-key"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Ensure database is deployed: cd db/terraform && terraform apply"
echo "  2. Ensure ML service is deployed: cd ml && ./scripts/deploy-ml.sh ${ENVIRONMENT}"
echo "  3. Deploy backend: ./scripts/deploy-backend.sh ${ENVIRONMENT}"
echo ""
echo -e "${YELLOW}View Secrets:${NC}"
echo "  gcloud secrets versions access latest --secret=mkrew-jwt-secret"
echo "  gcloud secrets versions access latest --secret=mkrew-scraper-api-key"
