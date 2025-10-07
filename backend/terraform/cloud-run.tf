# Terraform configuration for Backend API on Cloud Run
# This creates backend service with Cloud SQL, ML service integration

terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
}

# Variables
variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "europe-central2"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "cloud_sql_instance" {
  description = "Cloud SQL instance connection name"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "mkrew"
}

variable "db_user" {
  description = "Database user"
  type        = string
  default     = "mkrew"
}

variable "ml_service_url" {
  description = "ML Service Cloud Run URL"
  type        = string
}

variable "min_instances" {
  description = "Minimum number of instances"
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 10
}

# Provider
provider "google" {
  project = var.project_id
  region  = var.region
}

# JWT Secret
resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

# Scraper API Key
resource "random_password" "scraper_api_key" {
  length  = 64
  special = true
}

# Store JWT secret in Secret Manager
resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "mkrew-jwt-secret"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "jwt_secret_version" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = random_password.jwt_secret.result
}

# Store Scraper API key in Secret Manager
resource "google_secret_manager_secret" "scraper_api_key" {
  secret_id = "mkrew-scraper-api-key"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "scraper_api_key_version" {
  secret      = google_secret_manager_secret.scraper_api_key.id
  secret_data = random_password.scraper_api_key.result
}

# Grant backend service account access to secrets
resource "google_secret_manager_secret_iam_member" "backend_jwt_accessor" {
  secret_id = google_secret_manager_secret.jwt_secret.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
}

resource "google_secret_manager_secret_iam_member" "backend_scraper_key_accessor" {
  secret_id = google_secret_manager_secret.scraper_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
}

# Backend needs access to ML API key
data "google_secret_manager_secret" "ml_api_key" {
  secret_id = "mkrew-ml-api-key"
}

resource "google_secret_manager_secret_iam_member" "backend_ml_key_accessor" {
  secret_id = data.google_secret_manager_secret.ml_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
}

# Backend needs access to DB password
data "google_secret_manager_secret" "db_password" {
  secret_id = "mkrew-db-password"
}

resource "google_secret_manager_secret_iam_member" "backend_db_accessor" {
  secret_id = data.google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
}

# Grant Cloud Build access to secrets
resource "google_secret_manager_secret_iam_member" "cloudbuild_jwt_accessor" {
  secret_id = google_secret_manager_secret.jwt_secret.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

resource "google_secret_manager_secret_iam_member" "cloudbuild_scraper_accessor" {
  secret_id = google_secret_manager_secret.scraper_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Grant backend Cloud SQL Client role
resource "google_project_iam_member" "backend_cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
}

# Grant Cloud Build Cloud SQL Client role (for migrations)
resource "google_project_iam_member" "cloudbuild_cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Grant Cloud Build Cloud Run Admin (for deployment)
resource "google_project_iam_member" "cloudbuild_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Grant Cloud Build service account user for backend SA
resource "google_service_account_iam_member" "cloudbuild_backend_sa_user" {
  service_account_id = "projects/${var.project_id}/serviceAccounts/mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Enable required APIs
resource "google_project_service" "required_apis" {
  for_each = toset([
    "run.googleapis.com",
    "cloudbuild.googleapis.com",
    "secretmanager.googleapis.com",
    "sqladmin.googleapis.com",
    "sql-component.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# Outputs
output "jwt_secret_id" {
  description = "Secret Manager ID for JWT secret"
  value       = google_secret_manager_secret.jwt_secret.secret_id
}

output "scraper_api_key_secret_id" {
  description = "Secret Manager ID for Scraper API key"
  value       = google_secret_manager_secret.scraper_api_key.secret_id
}

output "jwt_secret" {
  description = "JWT Secret (sensitive)"
  value       = random_password.jwt_secret.result
  sensitive   = true
}

output "scraper_api_key" {
  description = "Scraper API Key (sensitive)"
  value       = random_password.scraper_api_key.result
  sensitive   = true
}

output "deployment_command" {
  description = "Command to deploy backend"
  value = <<-EOT
    gcloud builds submit \
      --config=backend/cloudbuild.yaml \
      --project=${var.project_id} \
      --substitutions=_ENVIRONMENT=${var.environment},_REGION=${var.region},_CLOUD_SQL_INSTANCE=${var.cloud_sql_instance},_ML_SERVICE_URL=${var.ml_service_url}
  EOT
}

output "backend_service_account" {
  description = "Backend service account email"
  value       = "mkrew-backend-sa@${var.project_id}.iam.gserviceaccount.com"
}
