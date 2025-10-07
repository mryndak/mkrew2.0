# Terraform configuration for ML Service on Cloud Run
# This creates Cloud Run service with authentication and IAM configuration

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

# Random API key for ML service
resource "random_password" "ml_api_key" {
  length  = 64
  special = true
}

# Service Account for ML Service
resource "google_service_account" "ml_service" {
  account_id   = "mkrew-ml-sa"
  display_name = "ML Service Account"
  description  = "Service account for ML forecasting service"
}

# Service Account for Backend Service
resource "google_service_account" "backend_service" {
  account_id   = "mkrew-backend-sa"
  display_name = "Backend Service Account"
  description  = "Service account for Backend API service"
}

# Store ML API key in Secret Manager
resource "google_secret_manager_secret" "ml_api_key" {
  secret_id = "mkrew-ml-api-key"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "ml_api_key_version" {
  secret      = google_secret_manager_secret.ml_api_key.id
  secret_data = random_password.ml_api_key.result
}

# Grant ML service account access to read its own API key
resource "google_secret_manager_secret_iam_member" "ml_secret_accessor" {
  secret_id = google_secret_manager_secret.ml_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.ml_service.email}"
}

# Grant Backend service account access to read ML API key
resource "google_secret_manager_secret_iam_member" "backend_secret_accessor" {
  secret_id = google_secret_manager_secret.ml_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend_service.email}"
}

# Grant Backend service account permission to invoke ML service (Cloud Run)
# This allows backend to call ML service endpoints
resource "google_project_iam_member" "backend_run_invoker" {
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.backend_service.email}"
}

# Grant Cloud Build access to Secret Manager
resource "google_secret_manager_secret_iam_member" "cloudbuild_secret_accessor" {
  secret_id = google_secret_manager_secret.ml_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Grant Cloud Build permission to deploy to Cloud Run
resource "google_project_iam_member" "cloudbuild_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Grant Cloud Build permission to act as service accounts
resource "google_service_account_iam_member" "cloudbuild_sa_user_ml" {
  service_account_id = google_service_account.ml_service.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

resource "google_service_account_iam_member" "cloudbuild_sa_user_backend" {
  service_account_id = google_service_account.backend_service.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Enable required APIs
resource "google_project_service" "required_apis" {
  for_each = toset([
    "run.googleapis.com",
    "cloudbuild.googleapis.com",
    "secretmanager.googleapis.com",
    "containerregistry.googleapis.com",
    "compute.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# Outputs
output "ml_service_account_email" {
  description = "Email of ML service account"
  value       = google_service_account.ml_service.email
}

output "backend_service_account_email" {
  description = "Email of Backend service account"
  value       = google_service_account.backend_service.email
}

output "ml_api_key_secret_id" {
  description = "Secret Manager ID for ML API key"
  value       = google_secret_manager_secret.ml_api_key.secret_id
}

output "ml_api_key" {
  description = "ML API Key (sensitive)"
  value       = random_password.ml_api_key.result
  sensitive   = true
}

output "deployment_command" {
  description = "Command to deploy ML service"
  value = <<-EOT
    gcloud builds submit \
      --config=ml/cloudbuild.yaml \
      --project=${var.project_id} \
      --substitutions=_ENVIRONMENT=${var.environment},_REGION=${var.region}
  EOT
}
