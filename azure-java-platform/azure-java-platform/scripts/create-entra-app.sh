#!/usr/bin/env bash
# Create the Entra ID app registration with two scopes (read/write).
set -euo pipefail
APP_NAME="${APP_NAME:-azure-java-platform}"

APP_ID=$(az ad app create --display-name "$APP_NAME" --sign-in-audience AzureADMyOrg --query appId -o tsv)
echo "Created app: $APP_ID"

az ad app update --id "$APP_ID" --identifier-uris "api://$APP_ID"

# Expose two scopes
TMP=$(mktemp)
cat > "$TMP" <<EOF
{
  "oauth2PermissionScopes": [
    {
      "adminConsentDescription": "Read access",
      "adminConsentDisplayName": "api.read",
      "id": "$(uuidgen)",
      "isEnabled": true,
      "type": "User",
      "value": "api.read"
    },
    {
      "adminConsentDescription": "Write access",
      "adminConsentDisplayName": "api.write",
      "id": "$(uuidgen)",
      "isEnabled": true,
      "type": "User",
      "value": "api.write"
    }
  ]
}
EOF
az ad app update --id "$APP_ID" --set api=@"$TMP"

az ad sp create --id "$APP_ID" >/dev/null
echo "Service principal created. Audience: api://$APP_ID"
