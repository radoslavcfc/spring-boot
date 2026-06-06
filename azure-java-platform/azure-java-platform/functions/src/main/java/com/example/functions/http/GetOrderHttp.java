package com.example.functions.http;

import com.example.functions.cosmos.CosmosClientHolder;
import com.example.functions.auth.JwtValidator;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/** Read processed order document from Cosmos by correlation id. */
public class GetOrderHttp {

    @FunctionName("GetOrder")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req",
            methods = {HttpMethod.GET},
            route = "orders/{id}",
            authLevel = AuthorizationLevel.FUNCTION)
        HttpRequestMessage<String> req,
        @com.microsoft.azure.functions.annotation.BindingName("id") String id,
        final ExecutionContext ctx) {

        if (JwtValidator.validate(req).isEmpty()) {
            return req.createResponseBuilder(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            JsonNode doc = CosmosClientHolder.container()
                .readItem(id, new PartitionKey(id), JsonNode.class)
                .block().getItem();
            return req.createResponseBuilder(HttpStatus.OK).body(doc).build();
        } catch (Exception e) {
            return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("Not found").build();
        }
    }
}
