#!/usr/bin/env bash
# Bootstrap remote Terraform state in an Azure Storage account.
set -euo pipefail
RG="${RG:-rg-tfstate}"
LOC="${LOC:-westeurope}"
SA="${SA:-tfstate$RANDOM$RANDOM}"
CONT="${CONT:-tfstate}"

az group create -n "$RG" -l "$LOC"
az storage account create -n "$SA" -g "$RG" -l "$LOC" --sku Standard_LRS --encryption-services blob
az storage container create -n "$CONT" --account-name "$SA"

cat > infra/backend.hcl <<EOF
resource_group_name  = "$RG"
storage_account_name = "$SA"
container_name       = "$CONT"
key                  = "azjava.tfstate"
EOF

echo "Wrote infra/backend.hcl"
