#!/bin/bash
#
# Setup infrastructure for ML service (service accounts, secrets, IAM)
# Usage: ./setup-infrastructure.sh [project_id] [environment]
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

echo -e "${GREEN}=== Setting up ML Service Infrastructure ===${NC}"
echo -e "${YELLOW}Project ID:${NC} ${PROJECT_ID}"
echo -e "${YELLOW}Environment:${NC} ${ENVIRONMENT}"
echo -e "${YELLOW}Region:${NC} ${REGION}"

# Confirm
read -p "Continue with setup? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo -e "${RED}Setup cancelled${NC}"
  exit 0
fi

# Set project
gcloud config set project "${PROJECT_ID}"

echo -e "${BLUE}Step 1: Enabling APIs...${NC}"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  containerregistry.googleapis.com \
  compute.googleapis.com \
  iam.googleapis.com

echo -e "${GREEN}✓ APIs enabled${NC}"

echo -e "${BLUE}Step 2: Creating service accounts...${NC}"

# ML Service Account
if gcloud iam service-accounts describe "mkrew-ml-sa@${PROJECT_ID}.iam.gserviceaccount.com" &>/dev/null; then
  echo "ML service account already exists"
else
  gcloud iam service-accounts create mkrew-ml-sa \
    --display-name="ML Service Account" \
    --description="Service account for ML forecasting service"
  echo -e "${GREEN}✓ ML service account created${NC}"
fi

# Backend Service Account
if gcloud iam service-accounts describe "mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" &>/dev/null; then
  echo "Backend service account already exists"
else
  gcloud iam service-accounts create mkrew-backend-sa \
    --display-name="Backend Service Account" \
    --description="Service account for Backend API service"
  echo -e "${GREEN}✓ Backend service account created${NC}"
fi

echo -e "${BLUE}Step 3: Generating ML API key...${NC}"

# Generate secure API key
ML_API_KEY=$(openssl rand -base64 48)

# Create or update secret
if gcloud secrets describe mkrew-ml-api-key &>/dev/null; then
  echo "Secret already exists, adding new version..."
  echo -n "$ML_API_KEY" | gcloud secrets versions add mkrew-ml-api-key --data-file=-
else
  echo -n "$ML_API_KEY" | gcloud secrets create mkrew-ml-api-key \
    --data-file=- \
    --replication-policy=automatic
  echo -e "${GREEN}✓ Secret created${NC}"
fi

echo -e "${GREEN}✓ ML API Key stored in Secret Manager${NC}"
echo -e "${YELLOW}API Key (save this for local development):${NC}"
echo "$ML_API_KEY"
echo ""

echo -e "${BLUE}Step 4: Configuring IAM permissions...${NC}"

# Grant ML service account access to its secret
gcloud secrets add-iam-policy-binding mkrew-ml-api-key \
  --member="serviceAccount:mkrew-ml-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Grant Backend service account access to ML secret
gcloud secrets add-iam-policy-binding mkrew-ml-api-key \
  --member="serviceAccount:mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Grant Cloud Build access to secrets
gcloud secrets add-iam-policy-binding mkrew-ml-api-key \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Grant Cloud Build permission to deploy to Cloud Run
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"

# Grant Cloud Build permission to act as service accounts
gcloud iam service-accounts add-iam-policy-binding \
  "mkrew-ml-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

gcloud iam service-accounts add-iam-policy-binding \
  "mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --member="serviceAccount:${PROJECT_ID}@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

echo -e "${GREEN}✓ IAM permissions configured${NC}"

echo -e "${BLUE}Step 5: Summary${NC}"
echo ""
echo -e "${GREEN}=== Infrastructure Setup Complete ===${NC}"
echo ""
echo -e "${YELLOW}Service Accounts:${NC}"
echo "  - ML: mkrew-ml-sa@${PROJECT_ID}.iam.gserviceaccount.com"
echo "  - Backend: mkrew-backend-sa@${PROJECT_ID}.iam.gserviceaccount.com"
echo ""
echo -e "${YELLOW}Secrets:${NC}"
echo "  - mkrew-ml-api-key (in Secret Manager)"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Deploy ML service: ./deploy-ml.sh ${ENVIRONMENT}"
echo "  2. Configure backend with ML service URL and API key"
echo "  3. Test end-to-end: Backend → ML Service"
echo ""
echo -e "${YELLOW}View API Key:${NC}"
echo "  gcloud secrets versions access latest --secret=mkrew-ml-api-key"
