variable "project"         { type = string  default = "azjava" }
variable "environment"     { type = string  default = "dev" }
variable "location"        { type = string  default = "westeurope" }
variable "sql_admin_login" { type = string  default = "sqladmin" }
variable "sql_admin_password" {
  type      = string
  sensitive = true
  description = "Bootstrap SQL admin password. Apps use managed identity, not this."
}
variable "entra_audience" {
  type        = string
  description = "Audience claim expected in Entra-issued JWTs (e.g. api://<app-id>)."
}
