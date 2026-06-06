package com.example.functions.kafka;

import com.example.functions.common.Json;
import com.example.functions.cosmos.CosmosClientHolder;
import com.example.shared.events.OrderEvent;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.KafkaTrigger;

import java.time.Instant;

/**
 * Kafka (Event Hubs) trigger. Consumes orders enqueued by HTTP triggers,
 * processes asynchronously, persists result to Cosmos DB.
 */
public class OrderKafkaListener {

    @FunctionName("OrderKafkaListener")
    public void run(
        @KafkaTrigger(
            name = "kafkaTrigger",
            topic = "%EVENTHUBS_TOPIC%",
            brokerList = "%EVENTHUBS_BOOTSTRAP%",
            consumerGroup = "$Default",
            cardinality = Cardinality.ONE,
            dataType = "string"
        ) String message,
        final ExecutionContext ctx) {

        try {
            OrderEvent ev = Json.MAPPER.readValue(message, OrderEvent.class);

            ObjectNode doc = Json.MAPPER.createObjectNode();
            doc.put("id", ev.getCorrelationId().toString());
            doc.put("customerId", ev.getCustomerId());
            doc.put("sku", ev.getSku());
            doc.put("quantity", ev.getQuantity());
            doc.put("status", "PROCESSED");
            doc.put("processedAt", Instant.now().toString());

            CosmosClientHolder.container().upsertItem(doc).block();
            ctx.getLogger().info("Processed order " + ev.getCorrelationId());
        } catch (Exception e) {
            ctx.getLogger().severe("Kafka processing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
