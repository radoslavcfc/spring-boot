package com.example.functions.http;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Serves OpenAPI spec + a Swagger UI page for the Function HTTP endpoints. */
public class SwaggerHttp {

    @FunctionName("OpenApiSpec")
    public HttpResponseMessage spec(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET}, route = "openapi.yaml",
                     authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<String> req) {
        return req.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/yaml")
            .body(read("/openapi.yaml")).build();
    }

    @FunctionName("SwaggerUi")
    public HttpResponseMessage ui(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET}, route = "swagger",
                     authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<String> req) {
        return req.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "text/html")
            .body(read("/swagger.html")).build();
    }

    private String read(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { return "not found"; }
    }
}
