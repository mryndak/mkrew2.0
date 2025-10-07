#!/bin/bash
#
# Deploy ML service to Cloud Run
# Usage: ./deploy-ml.sh [environment]
#
# Environments: dev, staging, prod
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default environment
ENVIRONMENT="${1:-dev}"

echo -e "${GREEN}=== Deploying ML Service to ${ENVIRONMENT} ===${NC}"

# Load environment-specific configuration
case "$ENVIRONMENT" in
  dev)
    PROJECT_ID="${GCP_PROJECT_ID:-your-project-id}"
    REGION="${GCP_REGION:-europe-central2}"
    MIN_INSTANCES=0
    MAX_INSTANCES=3
    MEMORY="2Gi"
    CPU=2
    ;;
  staging)
    PROJECT_ID="${GCP_PROJECT_ID_STAGING:-your-project-id-staging}"
    REGION="${GCP_REGION:-europe-central2}"
    MIN_INSTANCES=1
    MAX_INSTANCES=5
    MEMORY="2Gi"
    CPU=2
    ;;
  prod)
    PROJECT_ID="${GCP_PROJECT_ID_PROD:-your-project-id-prod}"
    REGION="${GCP_REGION:-europe-central2}"
    MIN_INSTANCES=2
    MAX_INSTANCES=10
    MEMORY="4Gi"
    CPU=4
    ;;
  *)
    echo -e "${RED}Error: Invalid environment '${ENVIRONMENT}'. Use: dev, staging, or prod${NC}"
    exit 1
    ;;
esac

echo -e "${YELLOW}Project ID:${NC} ${PROJECT_ID}"
echo -e "${YELLOW}Region:${NC} ${REGION}"
echo -e "${YELLOW}Memory:${NC} ${MEMORY}"
echo -e "${YELLOW}CPU:${NC} ${CPU}"
echo -e "${YELLOW}Min Instances:${NC} ${MIN_INSTANCES}"
echo -e "${YELLOW}Max Instances:${NC} ${MAX_INSTANCES}"

# Confirm deployment (skip for dev)
if [ "$ENVIRONMENT" != "dev" ]; then
  read -p "Are you sure you want to deploy ML service to ${ENVIRONMENT}? (yes/no): " CONFIRM
  if [ "$CONFIRM" != "yes" ]; then
    echo -e "${RED}Deployment cancelled${NC}"
    exit 0
  fi
fi

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    exit 1
fi

# Set gcloud project
gcloud config set project "${PROJECT_ID}"

# Trigger Cloud Build
echo -e "${GREEN}Triggering Cloud Build...${NC}"

gcloud builds submit \
  --config=ml/cloudbuild.yaml \
  --project="${PROJECT_ID}" \
  --substitutions=\
_ENVIRONMENT="${ENVIRONMENT}",\
_REGION="${REGION}",\
_MEMORY="${MEMORY}",\
_CPU="${CPU}",\
_MIN_INSTANCES="${MIN_INSTANCES}",\
_MAX_INSTANCES="${MAX_INSTANCES}" \
  .

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ ML Service deployed successfully to ${ENVIRONMENT}!${NC}"

  # Get service URL
  SERVICE_URL=$(gcloud run services describe "mkrew-ml-${ENVIRONMENT}" \
    --region="${REGION}" \
    --project="${PROJECT_ID}" \
    --format='value(status.url)')

  echo -e "${BLUE}Service URL:${NC} ${SERVICE_URL}"
  echo -e "${BLUE}Health Check:${NC} ${SERVICE_URL}/health"

  # Test health endpoint
  echo -e "${YELLOW}Testing health endpoint...${NC}"
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${SERVICE_URL}/health")

  if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Health check passed (HTTP ${HTTP_CODE})${NC}"
  else
    echo -e "${RED}✗ Health check failed (HTTP ${HTTP_CODE})${NC}"
  fi

  # Instructions for backend
  echo ""
  echo -e "${BLUE}=== Backend Configuration ===${NC}"
  echo -e "Add this to backend configuration:"
  echo -e "${YELLOW}ML_SERVICE_URL:${NC} ${SERVICE_URL}"
  echo ""
  echo -e "Get ML API key from Secret Manager:"
  echo -e "${YELLOW}gcloud secrets versions access latest --secret=mkrew-ml-api-key --project=${PROJECT_ID}${NC}"

else
  echo -e "${RED}✗ ML Service deployment failed${NC}"
  exit 1
fi
