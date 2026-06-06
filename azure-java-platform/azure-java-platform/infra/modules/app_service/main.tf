variable "base" { type = string }
variable "suffix" { type = string }
variable "location" { type = string }
variable "resource_group_name" { type = string }
variable "app_insights_connection_string" { type = string  sensitive = true }
variable "sql_jdbc_url" { type = string }
variable "entra_tenant_id" { type = string }
variable "entra_audience" { type = string }
variable "tags" { type = map(string) }

resource "azurerm_service_plan" "plan" {
  name                = "asp-${var.base}-${var.suffix}"
  location            = var.location
  resource_group_name = var.resource_group_name
  os_type             = "Linux"
  sku_name            = "B1"
  tags                = var.tags
}

resource "azurerm_linux_web_app" "api" {
  name                = "app-${var.base}-${var.suffix}"
  location            = var.location
  resource_group_name = var.resource_group_name
  service_plan_id     = azurerm_service_plan.plan.id

  identity { type = "SystemAssigned" }

  site_config {
    always_on = true
    application_stack { java_version = "17"  java_server = "JAVA"  java_server_version = "17" }
  }

  app_settings = {
    "WEBSITES_PORT"                            = "8080"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"    = var.app_insights_connection_string
    "ApplicationInsightsAgent_EXTENSION_VERSION" = "~3"
    "XDT_MicrosoftApplicationInsights_Java"    = "1"
    "SQL_JDBC_URL"                             = var.sql_jdbc_url
    "ENTRA_ISSUER"                             = "https://login.microsoftonline.com/${var.entra_tenant_id}/v2.0"
    "ENTRA_AUDIENCE"                           = var.entra_audience
    "SPRING_PROFILES_ACTIVE"                   = "prod"
  }
  tags = var.tags
}

output "principal_id"     { value = azurerm_linux_web_app.api.identity[0].principal_id }
output "default_hostname" { value = azurerm_linux_web_app.api.default_hostname }
