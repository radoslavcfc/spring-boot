variable "webapp_principal_id"    { type = string }
variable "function_principal_id"  { type = string }
variable "cosmos_account_id"      { type = string }
variable "sql_server_id"          { type = string }
variable "sql_database_name"      { type = string }
variable "eventhubs_namespace_id" { type = string }
variable "key_vault_id"           { type = string }

# Cosmos DB Built-in Data Contributor
resource "azurerm_role_assignment" "fn_cosmos" {
  scope                = var.cosmos_account_id
  role_definition_name = "Cosmos DB Built-in Data Contributor"
  principal_id         = var.function_principal_id
}

# Event Hubs Data Owner for Functions (send + receive)
resource "azurerm_role_assignment" "fn_eventhubs" {
  scope                = var.eventhubs_namespace_id
  role_definition_name = "Azure Event Hubs Data Owner"
  principal_id         = var.function_principal_id
}

# Key Vault Secrets User for both
resource "azurerm_role_assignment" "api_kv" {
  scope                = var.key_vault_id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = var.webapp_principal_id
}
resource "azurerm_role_assignment" "fn_kv" {
  scope                = var.key_vault_id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = var.function_principal_id
}

# NOTE: SQL managed-identity user grants must be performed via T-SQL after deploy.
# See docs/POST_DEPLOY.md for the CREATE USER ... FROM EXTERNAL PROVIDER script.
