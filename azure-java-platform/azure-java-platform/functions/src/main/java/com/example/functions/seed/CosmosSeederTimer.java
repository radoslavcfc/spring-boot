package com.example.functions.seed;

import com.azure.cosmos.models.PartitionKey;
import com.example.functions.common.Json;
import com.example.functions.cosmos.CosmosClientHolder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.time.Instant;

/**
 * Cosmos seeder. Runs every 5 minutes, but the first action is to check for a
 * "_seeded" marker document. If present, exits immediately. So data is only
 * inserted on the very first run after deployment.
 */
public class CosmosSeederTimer {

    private static final String MARKER_ID = "_seeded";

    @FunctionName("CosmosSeederTimer")
    public void run(
        @TimerTrigger(name = "timer", schedule = "0 */5 * * * *", runOnStartup = true)
        String timerInfo,
        final ExecutionContext ctx) {

        try {
            try {
                CosmosClientHolder.container()
                    .readItem(MARKER_ID, new PartitionKey(MARKER_ID), ObjectNode.class)
                    .block();
                ctx.getLogger().fine("Cosmos already seeded; skipping.");
                return;
            } catch (Exception ignored) {
                // marker missing - proceed
            }

            for (int i = 1; i <= 3; i++) {
                ObjectNode doc = Json.MAPPER.createObjectNode();
                String id = "seed-order-" + i;
                doc.put("id", id);
                doc.put("customerId", "seed-customer-" + i);
                doc.put("sku", "SKU-00" + i);
                doc.put("quantity", i);
                doc.put("status", "SEED");
                doc.put("processedAt", Instant.now().toString());
                CosmosClientHolder.container().upsertItem(doc).block();
            }

            ObjectNode marker = Json.MAPPER.createObjectNode();
            marker.put("id", MARKER_ID);
            marker.put("seededAt", Instant.now().toString());
            CosmosClientHolder.container().upsertItem(marker).block();

            ctx.getLogger().info("Cosmos seed completed.");
        } catch (Exception e) {
            ctx.getLogger().severe("Cosmos seed failed: " + e.getMessage());
        }
    }
}
