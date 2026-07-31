package com.farm.workers.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Security / Auth Configuration              ║
 * ║  builder.Services.AddAuthentication()                    ║
 * ║    .AddJwtBearer(options => {                            ║
 * ║       options.Authority = "https://login.microsoftonline"║
 * ║       options.Audience = clientId                       ║
 * ║    })                                                    ║
 * ║  ──────────── IS EQUIVALENT TO ────────────              ║
 * ║  @Configuration class below with @Bean SecurityFilter   ║
 * ║  + application.properties settings                      ║
 * ║                                                          ║
 * ║  app.UseAuthentication()    →  configured in filterChain║
 * ║  app.UseAuthorization()     →  configured in filterChain║
 * ║  [Authorize]                →  @PreAuthorize            ║
 * ║  [AllowAnonymous]           →  .permitAll()             ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @Configuration  ≈  a class marked for DI registration in .NET Startup
 * @EnableWebSecurity  ≈  app.UseAuthentication() + app.UseAuthorization()
 * @EnableMethodSecurity  ≈  enables [Authorize] on methods (not just globally)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * @Value injects from application.properties/application.yml
     * ≈ IConfiguration["Azure:Entra:TenantId"] in .NET
     * or IOptions<AzureAdOptions>.Value.TenantId
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * SecurityFilterChain = the main security configuration bean
     * ≈ the entire Configure(IApplicationBuilder app) section of Startup.cs
     *
     * HttpSecurity.authorizeHttpRequests() ≈ app.UseAuthorization() +
     * policy-based auth in .NET
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST APIs (same recommendation in .NET for APIs)
            .csrf(csrf -> csrf.disable())

            // Stateless session - JWT means no server-side session
            // ≈ options.Events.OnValidatePrincipal (no server sessions)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL-based authorization rules (complementary to @PreAuthorize on methods)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no auth needed
                .requestMatchers("/actuator/health").permitAll()    // Health check
                .requestMatchers("/v3/api-docs/**").permitAll()     // OpenAPI spec
                .requestMatchers("/swagger-ui/**").permitAll()      // Swagger UI
                .requestMatchers("/swagger-ui.html").permitAll()

                // Everything else requires authentication
                // ≈ [Authorize] on base controller or global policy
                .anyRequest().authenticated()
            )

            // Configure JWT resource server  ≈  .AddJwtBearer() in .NET
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * JWT Decoder - validates token signature against Azure Entra ID's public keys
     * ≈  options.Authority / options.MetadataAddress in .NET AddJwtBearer()
     *
     * Azure Entra ID publishes its public keys at the JWK Set URI.
     * NimbusJwtDecoder fetches and caches these keys automatically.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Maps JWT claims to Spring Security authorities (roles/permissions)
     *
     * ≈ options.TokenValidationParameters.RoleClaimType in .NET
     *
     * Azure Entra ID puts scopes in "scp" claim  e.g., "workers.read workers.write"
     * This converter adds "SCOPE_" prefix: → "SCOPE_workers.read", "SCOPE_workers.write"
     *
     * So in controllers: @PreAuthorize("hasAuthority('SCOPE_workers.read')")
     * matches token with scp: "workers.read"
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Azure uses "scp" for delegated permissions, "roles" for app roles
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scp");
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        // The "name" comes from the "sub" (subject) claim = user's object ID in Entra
        // ≈ User.FindFirstValue(ClaimTypes.NameIdentifier)
        jwtAuthenticationConverter.setPrincipalClaimName("sub");

        return jwtAuthenticationConverter;
    }
}
