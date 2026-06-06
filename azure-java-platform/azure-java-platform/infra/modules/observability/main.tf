variable "base" { type = string }
variable "location" { type = string }
variable "resource_group_name" { type = string }
variable "tags" { type = map(string) }

resource "azurerm_log_analytics_workspace" "law" {
  name                = "law-${var.base}"
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  tags                = var.tags
}

resource "azurerm_application_insights" "ai" {
  name                = "appi-${var.base}"
  location            = var.location
  resource_group_name = var.resource_group_name
  workspace_id        = azurerm_log_analytics_workspace.law.id
  application_type    = "java"
  tags                = var.tags
}

output "app_insights_connection_string" { value = azurerm_application_insights.ai.connection_string  sensitive = true }
output "law_id"                          { value = azurerm_log_analytics_workspace.law.id }
