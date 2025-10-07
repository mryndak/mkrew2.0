#!/bin/bash
#
# Rollback Liquibase migration
# Usage: ./rollback-migration.sh [count] [environment]
#
# count: Number of changesets to rollback (default: 1)
# environment: dev, staging, prod (default: dev)
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ROLLBACK_COUNT="${1:-1}"
ENVIRONMENT="${2:-dev}"

echo -e "${YELLOW}=== Rolling back ${ROLLBACK_COUNT} changeset(s) in ${ENVIRONMENT} ===${NC}"

# Load configuration
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
    echo -e "${RED}Error: Invalid environment${NC}"
    exit 1
    ;;
esac

CLOUD_SQL_INSTANCE="${PROJECT_ID}:${REGION}:${INSTANCE_NAME}"

# Confirmation
echo -e "${RED}WARNING: This will rollback ${ROLLBACK_COUNT} migration(s) in ${ENVIRONMENT}${NC}"
read -p "Type 'ROLLBACK' to confirm: " CONFIRM

if [ "$CONFIRM" != "ROLLBACK" ]; then
  echo -e "${RED}Rollback cancelled${NC}"
  exit 0
fi

# Get DB password from Secret Manager
DB_PASSWORD=$(gcloud secrets versions access latest \
  --secret="mkrew-db-password" \
  --project="${PROJECT_ID}")

# Download Cloud SQL Proxy
if [ ! -f "./cloud_sql_proxy" ]; then
  echo "Downloading Cloud SQL Proxy..."
  wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud_sql_proxy
  chmod +x cloud_sql_proxy
fi

# Start Cloud SQL Proxy
./cloud_sql_proxy -instances="${CLOUD_SQL_INSTANCE}"=tcp:5432 &
PROXY_PID=$!
sleep 5

echo -e "${GREEN}Running Liquibase rollback...${NC}"

# Run Liquibase rollback
docker run --rm \
  --network="host" \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.30-alpine \
  --url="jdbc:postgresql://localhost:5432/mkrew" \
  --username="mkrew" \
  --password="${DB_PASSWORD}" \
  --changelog-file=changelog/db.changelog-master.yaml \
  rollbackCount "${ROLLBACK_COUNT}"

# Stop proxy
kill $PROXY_PID

echo -e "${GREEN}✓ Rollback completed${NC}"
