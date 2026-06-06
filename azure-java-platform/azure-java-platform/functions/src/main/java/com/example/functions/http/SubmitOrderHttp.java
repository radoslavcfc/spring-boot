package com.example.functions.http;

import com.example.functions.auth.JwtValidator;
import com.example.functions.common.Json;
import com.example.functions.kafka.KafkaSender;
import com.example.shared.events.OrderEvent;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP trigger: accepts an OrderEvent, validates Entra JWT, enqueues to Kafka,
 * returns 202 with correlation id. Processing happens asynchronously in OrderKafkaListener.
 */
public class SubmitOrderHttp {

    @FunctionName("SubmitOrder")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req",
            methods = {HttpMethod.POST},
            route = "orders",
            authLevel = AuthorizationLevel.FUNCTION)
        HttpRequestMessage<String> req,
        final ExecutionContext ctx) {

        if (JwtValidator.validate(req).isEmpty()) {
            return req.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .body("Invalid or missing Entra ID bearer token").build();
        }

        try {
            OrderEvent ev = Json.MAPPER.readValue(req.getBody(), OrderEvent.class);
            if (ev.getCorrelationId() == null) ev.setCorrelationId(UUID.randomUUID());
            ev.setSubmittedAt(Instant.now());

            try (KafkaSender sender = new KafkaSender()) {
                sender.send(ev.getCorrelationId().toString(), Json.MAPPER.writeValueAsString(ev));
            }

            ctx.getLogger().info("Enqueued order " + ev.getCorrelationId());
            return req.createResponseBuilder(HttpStatus.ACCEPTED)
                .header("Location", "/api/orders/" + ev.getCorrelationId())
                .body(Map.of("correlationId", ev.getCorrelationId(), "status", "QUEUED"))
                .build();
        } catch (Exception e) {
            ctx.getLogger().severe("Submit failed: " + e.getMessage());
            return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        }
    }
}
