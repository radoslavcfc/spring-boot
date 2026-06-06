variable "base" { type = string }
variable "suffix" { type = string }
variable "location" { type = string }
variable "resource_group_name" { type = string }
variable "tags" { type = map(string) }

resource "azurerm_eventhub_namespace" "ns" {
  name                = "ehns-${var.base}-${var.suffix}"
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = "Standard"
  capacity            = 1
  tags                = var.tags
}

resource "azurerm_eventhub" "orders" {
  name                = "orders"
  namespace_name      = azurerm_eventhub_namespace.ns.name
  resource_group_name = var.resource_group_name
  partition_count     = 4
  message_retention   = 1
}

resource "azurerm_eventhub_namespace_authorization_rule" "rw" {
  name                = "RootSenderListener"
  namespace_name      = azurerm_eventhub_namespace.ns.name
  resource_group_name = var.resource_group_name
  listen              = true
  send                = true
  manage              = false
}

output "namespace_id"        { value = azurerm_eventhub_namespace.ns.id }
output "kafka_bootstrap"     { value = "${azurerm_eventhub_namespace.ns.name}.servicebus.windows.net:9093" }
output "topic_name"          { value = azurerm_eventhub.orders.name }
output "connection_string"   { value = azurerm_eventhub_namespace_authorization_rule.rw.primary_connection_string  sensitive = true }
