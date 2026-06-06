# Post-deploy steps

After `terraform apply`, run the SQL grants in `DEPLOY.md` section 5.

## Granting the Function App data plane access on Cosmos
The Terraform `identity_grants` module assigns "Cosmos DB Built-in Data Contributor" — that's the
RBAC data-plane role. No further action needed; the Function App's managed identity can read/write
the `orders` container.

## Application Insights
Auto-instrumentation is enabled on both apps via the `ApplicationInsightsAgent_EXTENSION_VERSION`
setting. To raise log verbosity, add `APPLICATIONINSIGHTS_CONFIGURATION_CONTENT` with your JSON.
