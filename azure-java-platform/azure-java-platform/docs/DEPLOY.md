# Deploy to Azure

## 0. Prereqs
- Azure subscription with **Owner** on the target subscription (needed for role assignments)
- `az login` succeeded; `az account show` returns the right subscription
- An Azure DevOps organization + project for CI/CD (optional — you can also `terraform apply` and `az webapp deploy` manually)

## 1. Bootstrap remote Terraform state
```bash
bash scripts/bootstrap-tfstate.sh
# writes infra/backend.hcl
```

## 2. Create the Entra ID app registration
```bash
bash scripts/create-entra-app.sh
# prints "Audience: api://<app-id>"  -> copy into infra/envs/dev.tfvars (entra_audience)
```

## 3. Deploy infrastructure
```bash
cd infra
# Edit envs/dev.tfvars: sql_admin_password, entra_audience
terraform init -backend-config=backend.hcl
terraform plan  -var-file=envs/dev.tfvars
terraform apply -var-file=envs/dev.tfvars
```

Outputs include `webapi_url`, `function_app_url`, and connection details.

## 4. Build & deploy code (manual path)
```bash
mvn -B clean package
az webapp deploy -g rg-azjava-dev -n <webapi-name> --src-path webapi/target/webapi.jar --type jar
cd functions && mvn azure-functions:deploy
```

## 5. Post-deploy SQL grants (T-SQL)
Run as the AAD admin (configured by Terraform):
```sql
-- Grant the Web App's managed identity access to appdb
CREATE USER [<webapi-name>] FROM EXTERNAL PROVIDER;
ALTER ROLE db_datareader ADD MEMBER [<webapi-name>];
ALTER ROLE db_datawriter ADD MEMBER [<webapi-name>];
ALTER ROLE db_ddladmin   ADD MEMBER [<webapi-name>];  -- needed for Flyway

CREATE USER [<function-app-name>] FROM EXTERNAL PROVIDER;
ALTER ROLE db_datareader ADD MEMBER [<function-app-name>];
ALTER ROLE db_datawriter ADD MEMBER [<function-app-name>];
```

## 6. CI/CD (Azure DevOps)
1. Create a service connection of type **Azure Resource Manager → Workload identity federation (automatic)** named `azure-prod-wif`.
2. Push the repo. Run `pipelines/azure-pipelines.yml`.
3. Stages: Build → Infra (gated) → Deploy WebApi + Functions.

## 7. Verify
- Web API: `https://<webapi>.azurewebsites.net/swagger-ui.html`
- Functions: `https://<func>.azurewebsites.net/api/swagger`
- App Insights → Logs:
  ```
  traces | where cloud_RoleName in ("webapi","func-azjava-dev-...") | order by timestamp desc
  ```
