package com.example.functions.auth;

import com.example.functions.common.Env;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URL;
import java.util.Optional;

/**
 * Validates Entra ID JWT bearer tokens on Function HTTP triggers.
 * Verifies signature against tenant JWKS, issuer, and audience.
 */
public final class JwtValidator {
    private static volatile ConfigurableJWTProcessor<SecurityContext> processor;

    public static Optional<JWTClaimsSet> validate(HttpRequestMessage<?> req) {
        try {
            String auth = req.getHeaders().get("authorization");
            if (auth == null) auth = req.getHeaders().get("Authorization");
            if (auth == null || !auth.toLowerCase().startsWith("bearer ")) return Optional.empty();
            String token = auth.substring(7).trim();

            ConfigurableJWTProcessor<SecurityContext> p = processor();
            JWTClaimsSet claims = p.process(token, null);

            String tenant = Env.get("ENTRA_TENANT_ID");
            String expectedIssuer = "https://login.microsoftonline.com/" + tenant + "/v2.0";
            String expectedAudience = Env.get("ENTRA_AUDIENCE");

            if (!expectedIssuer.equals(claims.getIssuer())) return Optional.empty();
            if (claims.getAudience() == null || !claims.getAudience().contains(expectedAudience)) return Optional.empty();

            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static ConfigurableJWTProcessor<SecurityContext> processor() throws Exception {
        if (processor == null) {
            synchronized (JwtValidator.class) {
                if (processor == null) {
                    String tenant = Env.get("ENTRA_TENANT_ID");
                    URL jwks = new URL("https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys");
                    JWKSource<SecurityContext> src = JWKSourceBuilder.create(jwks).build();
                    DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
                    p.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, src));
                    processor = p;
                }
            }
        }
        return processor;
    }
}
