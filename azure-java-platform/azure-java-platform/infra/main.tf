terraform {
  required_version = ">= 1.6.0"
  required_providers {
    azurerm = { source = "hashicorp/azurerm", version = "~> 4.0" }
    azuread = { source = "hashicorp/azuread", version = "~> 3.0" }
    random  = { source = "hashicorp/random",  version = "~> 3.6" }
  }
  backend "azurerm" {
    # Configure via -backend-config or backend.hcl. See scripts/bootstrap-tfstate.sh.
  }
}

provider "azurerm" {
  features {}
}

provider "azuread" {}

data "azurerm_client_config" "current" {}

locals {
  base = "${var.project}-${var.environment}"
  tags = {
    project     = var.project
    environment = var.environment
    managed_by  = "terraform"
  }
}

resource "random_string" "sfx" {
  length  = 5
  upper   = false
  special = false
}

resource "azurerm_resource_group" "main" {
  name     = "rg-${local.base}"
  location = var.location
  tags     = local.tags
}

module "observability" {
  source              = "./modules/observability"
  base                = local.base
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags
}

module "key_vault" {
  source              = "./modules/key_vault"
  base                = local.base
  suffix              = random_string.sfx.result
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  tags                = local.tags
}

module "sql" {
  source              = "./modules/sql"
  base                = local.base
  suffix              = random_string.sfx.result
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  admin_login         = var.sql_admin_login
  admin_password      = var.sql_admin_password
  tags                = local.tags
}

module "cosmos" {
  source              = "./modules/cosmos"
  base                = local.base
  suffix              = random_string.sfx.result
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags
}

module "eventhubs" {
  source              = "./modules/eventhubs"
  base                = local.base
  suffix              = random_string.sfx.result
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags
}

module "app_service" {
  source                          = "./modules/app_service"
  base                            = local.base
  suffix                          = random_string.sfx.result
  location                        = var.location
  resource_group_name             = azurerm_resource_group.main.name
  app_insights_connection_string  = module.observability.app_insights_connection_string
  sql_jdbc_url                    = module.sql.jdbc_url
  entra_tenant_id                 = data.azurerm_client_config.current.tenant_id
  entra_audience                  = var.entra_audience
  tags                            = local.tags
}

module "function_app" {
  source                          = "./modules/function_app"
  base                            = local.base
  suffix                          = random_string.sfx.result
  location                        = var.location
  resource_group_name             = azurerm_resource_group.main.name
  app_insights_connection_string  = module.observability.app_insights_connection_string
  cosmos_endpoint                 = module.cosmos.endpoint
  cosmos_database                 = module.cosmos.database_name
  cosmos_container                = module.cosmos.container_name
  eventhubs_bootstrap             = module.eventhubs.kafka_bootstrap
  eventhubs_connection_string     = module.eventhubs.connection_string
  eventhubs_topic                 = module.eventhubs.topic_name
  sql_jdbc_url                    = module.sql.jdbc_url
  entra_tenant_id                 = data.azurerm_client_config.current.tenant_id
  entra_audience                  = var.entra_audience
  tags                            = local.tags
}

module "identity_grants" {
  source                       = "./modules/identity_grants"
  webapp_principal_id          = module.app_service.principal_id
  function_principal_id        = module.function_app.principal_id
  cosmos_account_id            = module.cosmos.account_id
  sql_server_id                = module.sql.server_id
  sql_database_name            = module.sql.database_name
  eventhubs_namespace_id       = module.eventhubs.namespace_id
  key_vault_id                 = module.key_vault.id
}

output "webapi_url"        { value = module.app_service.default_hostname }
output "function_app_url"  { value = module.function_app.default_hostname }
output "cosmos_endpoint"   { value = module.cosmos.endpoint }
output "sql_server_fqdn"   { value = module.sql.server_fqdn }
output "eventhubs_kafka"   { value = module.eventhubs.kafka_bootstrap }
