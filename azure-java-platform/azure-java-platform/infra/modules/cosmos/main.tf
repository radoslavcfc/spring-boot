variable "base" { type = string }
variable "suffix" { type = string }
variable "location" { type = string }
variable "resource_group_name" { type = string }
variable "tags" { type = map(string) }

resource "azurerm_cosmosdb_account" "acct" {
  name                = "cosmos-${var.base}-${var.suffix}"
  location            = var.location
  resource_group_name = var.resource_group_name
  offer_type          = "Standard"
  kind                = "GlobalDocumentDB"
  consistency_policy { consistency_level = "Session" }
  geo_location {
    location          = var.location
    failover_priority = 0
  }
  tags = var.tags
}

resource "azurerm_cosmosdb_sql_database" "db" {
  name                = "appdb"
  resource_group_name = var.resource_group_name
  account_name        = azurerm_cosmosdb_account.acct.name
  throughput          = 400
}

resource "azurerm_cosmosdb_sql_container" "orders" {
  name                  = "orders"
  resource_group_name   = var.resource_group_name
  account_name          = azurerm_cosmosdb_account.acct.name
  database_name         = azurerm_cosmosdb_sql_database.db.name
  partition_key_paths   = ["/id"]
  partition_key_version = 2
}

output "account_id"      { value = azurerm_cosmosdb_account.acct.id }
output "endpoint"        { value = azurerm_cosmosdb_account.acct.endpoint }
output "database_name"   { value = azurerm_cosmosdb_sql_database.db.name }
output "container_name"  { value = azurerm_cosmosdb_sql_container.orders.name }
