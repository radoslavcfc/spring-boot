package com.example.webapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        final String name = "EntraOAuth";
        return new OpenAPI()
            .info(new Info().title("Web API").version("1.0.0")
                .description("Spring Boot Web API protected by Entra ID. Long-running ops via 202+polling."))
            .addSecurityItem(new SecurityRequirement().addList(name))
            .components(new Components().addSecuritySchemes(name,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
