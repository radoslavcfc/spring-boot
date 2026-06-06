variable "base" { type = string }
variable "suffix" { type = string }
variable "location" { type = string }
variable "resource_group_name" { type = string }
variable "admin_login" { type = string }
variable "admin_password" { type = string  sensitive = true }
variable "tags" { type = map(string) }

resource "azurerm_mssql_server" "srv" {
  name                         = "sql-${var.base}-${var.suffix}"
  location                     = var.location
  resource_group_name          = var.resource_group_name
  version                      = "12.0"
  administrator_login          = var.admin_login
  administrator_login_password = var.admin_password
  minimum_tls_version          = "1.2"
  azuread_administrator {
    login_username = "AzureAD Admin"
    object_id      = data.azurerm_client_config.current.object_id
  }
  tags = var.tags
}

data "azurerm_client_config" "current" {}

resource "azurerm_mssql_database" "db" {
  name      = "appdb"
  server_id = azurerm_mssql_server.srv.id
  sku_name  = "S0"
  tags      = var.tags
}

resource "azurerm_mssql_firewall_rule" "azure" {
  name             = "allow-azure"
  server_id        = azurerm_mssql_server.srv.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

output "server_id"     { value = azurerm_mssql_server.srv.id }
output "server_fqdn"   { value = azurerm_mssql_server.srv.fully_qualified_domain_name }
output "database_name" { value = azurerm_mssql_database.db.name }
output "jdbc_url" {
  value = "jdbc:sqlserver://${azurerm_mssql_server.srv.fully_qualified_domain_name}:1433;database=appdb;encrypt=true;authentication=ActiveDirectoryMSI;"
}
