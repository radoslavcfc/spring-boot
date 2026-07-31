package com.farm.workers.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Swagger / OpenAPI Setup                    ║
 * ║  builder.Services.AddSwaggerGen(c => {                  ║
 * ║    c.SwaggerDoc("v1", new OpenApiInfo { Title = "..." });║
 * ║    c.AddSecurityDefinition("Bearer", ...);              ║
 * ║  });                                                     ║
 * ║  app.UseSwagger(); app.UseSwaggerUI();                  ║
 * ║  ──────────── IS EQUIVALENT TO ────────────              ║
 * ║  @Bean OpenAPI customOpenAPI() below                    ║
 * ║  SpringDoc auto-configures the UI endpoints             ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Swagger UI available at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON spec at:    http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${azure.ad.tenant-id}")
    private String tenantId;

    @Value("${azure.ad.client-id}")
    private String clientId;

    @Bean
    public OpenAPI customOpenAPI() {
        // OAuth2 authorization URL for Azure Entra ID
        String authorizationUrl = String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize", tenantId);
        String tokenUrl = String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);

        return new OpenAPI()
                .info(new Info()
                        .title("Farm Seasonal Workers API")
                        .description("""
                            REST API for managing farm seasonal workers and their work records.
                            
                            **Authentication:** Azure Entra ID (OAuth2 / JWT Bearer tokens)
                            
                            **How to get a token for testing:**
                            ```
                            curl -X POST https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token \\
                              -d "client_id={clientId}&client_secret={secret}&grant_type=client_credentials&scope=api://{clientId}/.default"
                            ```
                            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Farm API Team")
                                .email("api@farm.com"))
                        .license(new License().name("Internal")))

                // Security scheme: Bearer JWT  ≈  AddSecurityDefinition("Bearer", ...) in .NET
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your Azure Entra ID JWT token"))
                        // OAuth2 flow for interactive Swagger auth
                        .addSecuritySchemes("oauth2",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(authorizationUrl)
                                                        .tokenUrl(tokenUrl)))))

                // Apply security globally  ≈  c.AddSecurityRequirement(...) in .NET
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
