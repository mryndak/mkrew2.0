#!/bin/bash
#
# Deploy Liquibase migrations to Cloud SQL PostgreSQL
# Usage: ./deploy-migrations.sh [environment]
#
# Environments: dev, staging, prod
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default environment
ENVIRONMENT="${1:-dev}"

echo -e "${GREEN}=== Deploying Database Migrations to ${ENVIRONMENT} ===${NC}"

# Load environment-specific configuration
case "$ENVIRONMENT" in
  dev)
    PROJECT_ID="${GCP_PROJECT_ID:-your-project-id}"
    REGION="${GCP_REGION:-europe-central2}"
    INSTANCE_NAME="mkrew-postgres-${REGION}"
    ;;
  staging)
    PROJECT_ID="${GCP_PROJECT_ID_STAGING:-your-project-id-staging}"
    REGION="${GCP_REGION:-europe-central2}"
    INSTANCE_NAME="mkrew-postgres-staging-${REGION}"
    ;;
  prod)
    PROJECT_ID="${GCP_PROJECT_ID_PROD:-your-project-id-prod}"
    REGION="${GCP_REGION:-europe-central2}"
    INSTANCE_NAME="mkrew-postgres-prod-${REGION}"
    ;;
  *)
    echo -e "${RED}Error: Invalid environment '${ENVIRONMENT}'. Use: dev, staging, or prod${NC}"
    exit 1
    ;;
esac

CLOUD_SQL_INSTANCE="${PROJECT_ID}:${REGION}:${INSTANCE_NAME}"
DB_NAME="mkrew"
DB_USER="mkrew"

echo -e "${YELLOW}Project ID:${NC} ${PROJECT_ID}"
echo -e "${YELLOW}Region:${NC} ${REGION}"
echo -e "${YELLOW}Instance:${NC} ${INSTANCE_NAME}"
echo -e "${YELLOW}Database:${NC} ${DB_NAME}"

# Confirm deployment (skip for dev)
if [ "$ENVIRONMENT" != "dev" ]; then
  read -p "Are you sure you want to deploy to ${ENVIRONMENT}? (yes/no): " CONFIRM
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

# Trigger Cloud Build
echo -e "${GREEN}Triggering Cloud Build...${NC}"

gcloud builds submit \
  --config=db/cloudbuild.yaml \
  --project="${PROJECT_ID}" \
  --substitutions=\
_CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE}",\
_DB_NAME="${DB_NAME}",\
_DB_USER="${DB_USER}" \
  .

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Migrations deployed successfully to ${ENVIRONMENT}!${NC}"
else
  echo -e "${RED}✗ Migration deployment failed${NC}"
  exit 1
fi
