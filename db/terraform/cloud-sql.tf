# Terraform configuration for Cloud SQL PostgreSQL instance
# This creates a PostgreSQL database for mkrew2 application on GCP

terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
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

variable "database_name" {
  description = "Database name"
  type        = string
  default     = "mkrew"
}

variable "database_user" {
  description = "Database user"
  type        = string
  default     = "mkrew"
}

variable "database_tier" {
  description = "Cloud SQL instance tier"
  type        = string
  default     = "db-f1-micro"  # Free tier eligible
}

# Provider configuration
provider "google" {
  project = var.project_id
  region  = var.region
}

# Random password for database
resource "random_password" "db_password" {
  length  = 32
  special = true
}

# Cloud SQL PostgreSQL Instance
resource "google_sql_database_instance" "mkrew_postgres" {
  name             = "mkrew-postgres-${var.region}"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier              = var.database_tier
    availability_type = "ZONAL"  # Use REGIONAL for production
    disk_type         = "PD_SSD"
    disk_size         = 10  # GB
    disk_autoresize   = true

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = true
      transaction_log_retention_days = 7
      backup_retention_settings {
        retained_backups = 7
      }
    }

    ip_configuration {
      ipv4_enabled    = true
      private_network = null  # Use VPC if needed
      require_ssl     = true

      # Allow Cloud Build to access
      authorized_networks {
        name  = "cloud-build"
        value = "0.0.0.0/0"  # Restrict this in production
      }
    }

    database_flags {
      name  = "max_connections"
      value = "100"
    }

    database_flags {
      name  = "shared_buffers"
      value = "256000"  # KB
    }
  }

  deletion_protection = true  # Prevent accidental deletion

  lifecycle {
    prevent_destroy = true
  }
}

# Database
resource "google_sql_database" "mkrew_db" {
  name     = var.database_name
  instance = google_sql_database_instance.mkrew_postgres.name
}

# Database User
resource "google_sql_user" "mkrew_user" {
  name     = var.database_user
  instance = google_sql_database_instance.mkrew_postgres.name
  password = random_password.db_password.result
}

# Store password in Secret Manager
resource "google_secret_manager_secret" "db_password" {
  secret_id = "mkrew-db-password"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_password_version" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_password.result
}

# Grant Cloud Build access to Secret Manager
resource "google_secret_manager_secret_iam_member" "cloudbuild_secret_accessor" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Grant Cloud Build access to Cloud SQL
resource "google_project_iam_member" "cloudbuild_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${var.project_id}@cloudbuild.gserviceaccount.com"
}

# Outputs
output "instance_connection_name" {
  description = "Connection name for Cloud SQL instance"
  value       = google_sql_database_instance.mkrew_postgres.connection_name
}

output "instance_ip_address" {
  description = "IP address of Cloud SQL instance"
  value       = google_sql_database_instance.mkrew_postgres.public_ip_address
}

output "database_name" {
  description = "Database name"
  value       = google_sql_database.mkrew_db.name
}

output "database_user" {
  description = "Database user"
  value       = google_sql_user.mkrew_user.name
  sensitive   = true
}

output "secret_id" {
  description = "Secret Manager secret ID for database password"
  value       = google_secret_manager_secret.db_password.secret_id
}
