#!/bin/bash
#
# Deploy Backend API to Cloud Run
# Usage: ./deploy-backend.sh [environment]
#
# Environments: dev, staging, prod
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENVIRONMENT="${1:-dev}"

echo -e "${GREEN}=== Deploying Backend API to ${ENVIRONMENT} ===${NC}"

# Load environment-specific configuration
case "$ENVIRONMENT" in
  dev)
    PROJECT_ID="${GCP_PROJECT_ID:-your-project-id}"
    REGION="${GCP_REGION:-europe-central2}"
    MIN_INSTANCES=0
    MAX_INSTANCES=5
    MEMORY="1Gi"
    CPU=2
    ;;
  staging)
    PROJECT_ID="${GCP_PROJECT_ID_STAGING:-your-project-id-staging}"
    REGION="${GCP_REGION:-europe-central2}"
    MIN_INSTANCES=1
    MAX_INSTANCES=10
    MEMORY="2Gi"
    CPU=2
    ;;
  prod)
    PROJECT_ID="${GCP_PROJECT_ID_PROD:-your-project-id-prod}"
    REGION="${GCP_REGION:-europe-central2}"
    MIN_INSTANCES=2
    MAX_INSTANCES=20
    MEMORY="2Gi"
    CPU=4
    ;;
  *)
    echo -e "${RED}Error: Invalid environment '${ENVIRONMENT}'${NC}"
    exit 1
    ;;
esac

# Get Cloud SQL instance connection name
CLOUD_SQL_INSTANCE=$(gcloud sql instances describe "mkrew-postgres-${REGION}" \
  --project="${PROJECT_ID}" \
  --format='value(connectionName)' 2>/dev/null || echo "")

if [ -z "$CLOUD_SQL_INSTANCE" ]; then
  echo -e "${RED}Error: Cloud SQL instance not found${NC}"
  echo "Please create Cloud SQL instance first: cd db/terraform && terraform apply"
  exit 1
fi

# Get ML Service URL
ML_SERVICE_URL=$(gcloud run services describe "mkrew-ml-${ENVIRONMENT}" \
  --region="${REGION}" \
  --project="${PROJECT_ID}" \
  --format='value(status.url)' 2>/dev/null || echo "")

if [ -z "$ML_SERVICE_URL" ]; then
  echo -e "${YELLOW}Warning: ML Service not found. Backend will deploy without ML integration.${NC}"
  ML_SERVICE_URL="http://localhost:5000"
fi

echo -e "${YELLOW}Project ID:${NC} ${PROJECT_ID}"
echo -e "${YELLOW}Region:${NC} ${REGION}"
echo -e "${YELLOW}Cloud SQL:${NC} ${CLOUD_SQL_INSTANCE}"
echo -e "${YELLOW}ML Service:${NC} ${ML_SERVICE_URL}"
echo -e "${YELLOW}Memory:${NC} ${MEMORY}"
echo -e "${YELLOW}CPU:${NC} ${CPU}"
echo -e "${YELLOW}Instances:${NC} ${MIN_INSTANCES}-${MAX_INSTANCES}"

# Confirm deployment
if [ "$ENVIRONMENT" != "dev" ]; then
  read -p "Deploy backend to ${ENVIRONMENT}? (yes/no): " CONFIRM
  if [ "$CONFIRM" != "yes" ]; then
    echo -e "${RED}Deployment cancelled${NC}"
    exit 0
  fi
fi

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI not installed${NC}"
    exit 1
fi

# Set project
gcloud config set project "${PROJECT_ID}"

# Trigger Cloud Build
echo -e "${GREEN}Triggering Cloud Build...${NC}"

gcloud builds submit \
  --config=backend/cloudbuild.yaml \
  --project="${PROJECT_ID}" \
  --substitutions=\
_ENVIRONMENT="${ENVIRONMENT}",\
_REGION="${REGION}",\
_CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE}",\
_DB_NAME="mkrew",\
_DB_USER="mkrew",\
_ML_SERVICE_URL="${ML_SERVICE_URL}",\
_MEMORY="${MEMORY}",\
_CPU="${CPU}",\
_MIN_INSTANCES="${MIN_INSTANCES}",\
_MAX_INSTANCES="${MAX_INSTANCES}" \
  .

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Backend deployed successfully!${NC}"

  # Get service URL
  SERVICE_URL=$(gcloud run services describe "mkrew-backend-${ENVIRONMENT}" \
    --region="${REGION}" \
    --project="${PROJECT_ID}" \
    --format='value(status.url)')

  echo ""
  echo -e "${BLUE}=== Backend API Deployed ===${NC}"
  echo -e "${YELLOW}Service URL:${NC} ${SERVICE_URL}"
  echo -e "${YELLOW}Health:${NC} ${SERVICE_URL}/actuator/health"
  echo -e "${YELLOW}API Docs:${NC} ${SERVICE_URL}/swagger-ui.html"
  echo ""

  # Test health
  echo -e "${YELLOW}Testing health endpoint...${NC}"
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${SERVICE_URL}/actuator/health")

  if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Health check passed (HTTP ${HTTP_CODE})${NC}"
  else
    echo -e "${RED}✗ Health check failed (HTTP ${HTTP_CODE})${NC}"
  fi

  # Display credentials
  echo ""
  echo -e "${BLUE}=== API Credentials ===${NC}"
  echo "Default users:"
  echo "  Admin: admin / admin123"
  echo "  User:  user / user123"
  echo ""
  echo "Get JWT token:"
  echo -e "${YELLOW}curl -X POST ${SERVICE_URL}/api/auth/login \\
  -H 'Content-Type: application/json' \\
  -d '{\"username\":\"admin\",\"password\":\"admin123\"}'${NC}"

else
  echo -e "${RED}✗ Backend deployment failed${NC}"
  exit 1
fi
