variable "base" { type = string }
variable "suffix" { type = string }
variable "location" { type = string }
variable "resource_group_name" { type = string }
variable "app_insights_connection_string" { type = string  sensitive = true }
variable "cosmos_endpoint" { type = string }
variable "cosmos_database" { type = string }
variable "cosmos_container" { type = string }
variable "eventhubs_bootstrap" { type = string }
variable "eventhubs_connection_string" { type = string  sensitive = true }
variable "eventhubs_topic" { type = string }
variable "sql_jdbc_url" { type = string }
variable "entra_tenant_id" { type = string }
variable "entra_audience" { type = string }
variable "tags" { type = map(string) }

resource "azurerm_storage_account" "func" {
  name                     = "stfunc${var.suffix}${substr(replace(var.base,"-",""),0,8)}"
  resource_group_name      = var.resource_group_name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  tags                     = var.tags
}

resource "azurerm_service_plan" "plan" {
  name                = "asp-func-${var.base}-${var.suffix}"
  location            = var.location
  resource_group_name = var.resource_group_name
  os_type             = "Linux"
  sku_name            = "Y1"
  tags                = var.tags
}

resource "azurerm_linux_function_app" "fn" {
  name                       = "func-${var.base}-${var.suffix}"
  location                   = var.location
  resource_group_name        = var.resource_group_name
  service_plan_id            = azurerm_service_plan.plan.id
  storage_account_name       = azurerm_storage_account.func.name
  storage_account_access_key = azurerm_storage_account.func.primary_access_key

  identity { type = "SystemAssigned" }

  site_config {
    application_stack { java_version = "17" }
  }

  app_settings = {
    "FUNCTIONS_WORKER_RUNTIME"               = "java"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = var.app_insights_connection_string
    "COSMOS_ENDPOINT"                        = var.cosmos_endpoint
    "COSMOS_DATABASE"                        = var.cosmos_database
    "COSMOS_CONTAINER"                       = var.cosmos_container
    "EVENTHUBS_BOOTSTRAP"                    = var.eventhubs_bootstrap
    "EVENTHUBS_CONNECTION_STRING"            = var.eventhubs_connection_string
    "EVENTHUBS_TOPIC"                        = var.eventhubs_topic
    "EVENTHUBS_USE_SASL"                     = "true"
    "SQL_JDBC_URL"                           = var.sql_jdbc_url
    "ENTRA_TENANT_ID"                        = var.entra_tenant_id
    "ENTRA_AUDIENCE"                         = var.entra_audience
  }

  tags = var.tags
}

output "principal_id"     { value = azurerm_linux_function_app.fn.identity[0].principal_id }
output "default_hostname" { value = azurerm_linux_function_app.fn.default_hostname }
