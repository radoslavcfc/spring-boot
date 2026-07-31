package com.farm.functions.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Azure Functions Queue & CosmosDB Triggers  ║
 * ║                                                          ║
 * ║  Queue Trigger .NET:                                     ║
 * ║  [QueueTrigger("my-queue")] string message              ║
 * ║                                                          ║
 * ║  Queue Trigger Java:                                     ║
 * ║  @QueueTrigger(name="msg", queueName="my-queue",...) String msg ║
 * ║                                                          ║
 * ║  CosmosDB Trigger .NET:                                  ║
 * ║  [CosmosDBTrigger(databaseName: "db",...)]              ║
 * ║    IReadOnlyList<Document> input                        ║
 * ║                                                          ║
 * ║  CosmosDB Trigger Java:                                  ║
 * ║  @CosmosDBTrigger(name="docs", databaseName="db",...)  ║
 * ║    String documents                                      ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class WorkRecordFunctions {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    /**
     * QUEUE TRIGGER: Process work records created messages
     *
     * Triggered when the Spring Boot API puts a message in the
     * "work-records-created" Azure Storage Queue
     *
     * ≈ [QueueTrigger("work-records-created", Connection="...")] in .NET
     */
    @FunctionName("ProcessWorkRecordCreated")
    public void processWorkRecordCreated(
            @QueueTrigger(
                name = "message",
                queueName = "work-records-created",
                connection = "STORAGE_CONNECTION_STRING"
            ) String messageJson,  // Azure Functions auto-decodes Base64 queue messages

            final ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("Queue trigger: ProcessWorkRecordCreated");
        log.info("Message: " + messageJson);

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> message = objectMapper.readValue(messageJson, Map.class);

            String recordId = message.get("recordId");
            String workerId = message.get("workerId");

            log.info("Processing work record: " + recordId + " for worker: " + workerId);

            // Business logic:
            // 1. Validate no overlapping shifts for the same worker on same date
            // 2. Auto-calculate overtime if hours > 8
            // 3. Send notification to supervisor

            validateNoOverlap(recordId, workerId, log);
            notifySupervisor(workerId, recordId, log);

            log.info("Successfully processed work record: " + recordId);

        } catch (Exception e) {
            log.severe("Error processing work record message: " + e.getMessage());
            // Throwing here causes Azure Functions to retry
            // After max retries, message goes to poison queue
            // ≈ dead-letter queue behavior in Service Bus
            throw new RuntimeException("Processing failed, will retry", e);
        }
    }

    /**
     * SERVICE BUS TRIGGER: Process background check requests
     * More powerful than Queue trigger:
     * - Supports sessions (ordered processing per worker)
     * - Dead-letter queue built-in
     * - At-least-once delivery guarantee
     *
     * ≈ [ServiceBusTrigger("background-checks", Connection="...")] in .NET
     */
    @FunctionName("ProcessBackgroundCheck")
    public void processBackgroundCheck(
            @ServiceBusQueueTrigger(
                name = "message",
                queueName = "background-checks",
                connection = "SERVICEBUS_CONNECTION_STRING"
            ) String messageJson,
            final ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("Service Bus trigger: ProcessBackgroundCheck");

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> message = objectMapper.readValue(messageJson, Map.class);

            String workerId = message.get("workerId");
            String nationalId = message.get("nationalId");
            String nationality = message.get("nationality");

            log.info("Running background check for worker: " + workerId);

            // Simulate calling external background check API
            // (police records, work permit validity, etc.)
            boolean passed = runBackgroundCheck(nationalId, nationality, log);

            // Update worker status via CosmosDB SDK
            updateWorkerVerificationStatus(workerId, passed, log);

        } catch (Exception e) {
            log.severe("Background check failed: " + e.getMessage());
            throw new RuntimeException("Background check processing failed", e);
        }
    }

    /**
     * COSMOS DB TRIGGER: React to changes in the workers container
     *
     * CosmosDB Change Feed = stream of every create/update to documents
     * This function fires whenever a worker document is created or modified
     *
     * ≈ [CosmosDBTrigger(...)] in .NET - exactly the same concept!
     *
     * Use cases:
     * - Sync data to a search index (Azure Cognitive Search / Elasticsearch)
     * - Send welcome email on worker creation
     * - Audit logging
     * - Invalidate caches
     */
    @FunctionName("OnWorkerChanged")
    public void onWorkerChanged(
            @CosmosDBTrigger(
                name = "documents",
                databaseName = "farm-workers-db",
                containerName = "workers",
                connection = "COSMOS_CONNECTION_STRING",
                createLeaseContainerIfNotExists = true,
                leaseContainerName = "workers-leases"  // CosmosDB uses leases for change feed
            ) String documentsJson,  // JSON array of changed documents
            final ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("CosmosDB trigger: OnWorkerChanged");

        try {
            // documentsJson is a JSON array of changed/inserted documents
            // ≈ IReadOnlyList<Document> in .NET binding
            var documents = objectMapper.readTree(documentsJson);

            for (var doc : documents) {
                String workerId = doc.get("id").asText();
                String status = doc.has("status") ? doc.get("status").asText() : "UNKNOWN";

                log.info("Worker changed: " + workerId + " status: " + status);

                // Example reactions:
                if ("ACTIVE".equals(status)) {
                    // Index in search service for fast full-text search
                    indexWorkerInSearch(workerId, doc.toString(), log);
                }

                if ("INACTIVE".equals(status)) {
                    // Archive work records or send offboarding email
                    log.info("Worker deactivated, scheduling offboarding: " + workerId);
                }
            }
        } catch (Exception e) {
            log.severe("Error processing CosmosDB change feed: " + e.getMessage());
        }
    }

    /**
     * COSMOS DB TRIGGER: React to changes in work-records container
     * Used for real-time analytics aggregation
     */
    @FunctionName("OnWorkRecordChanged")
    public void onWorkRecordChanged(
            @CosmosDBTrigger(
                name = "records",
                databaseName = "farm-workers-db",
                containerName = "work-records",
                connection = "COSMOS_CONNECTION_STRING",
                createLeaseContainerIfNotExists = true,
                leaseContainerName = "work-records-leases"
            ) String documentsJson,

            // Output binding: write aggregated stats to a different container
            // ≈ [CosmosDB("db", "stats-container", Connection="...")] out T[] output
            @CosmosDBOutput(
                name = "statsOutput",
                databaseName = "farm-workers-db",
                containerName = "season-stats",
                connection = "COSMOS_CONNECTION_STRING",
                createIfNotExists = true
            ) OutputBinding<String> statsOutput,

            final ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("CosmosDB trigger: OnWorkRecordChanged - updating season stats");

        try {
            var documents = objectMapper.readTree(documentsJson);

            for (var doc : documents) {
                String workerId = doc.has("workerId") ? doc.get("workerId").asText() : null;
                String season = doc.has("season") ? doc.get("season").asText() : null;

                if (workerId != null && season != null) {
                    // Re-compute season stats and persist via output binding
                    String stats = recomputeSeasonStats(workerId, season, log);
                    statsOutput.setValue(stats);
                }
            }
        } catch (Exception e) {
            log.severe("Error processing work record change feed: " + e.getMessage());
        }
    }

    // ─── Private helper methods ───────────────────────────────

    private void validateNoOverlap(String recordId, String workerId, Logger log) {
        // In reality: query CosmosDB for records on same date for same worker
        log.info("Validating no overlapping shifts for record: " + recordId);
    }

    private void notifySupervisor(String workerId, String recordId, Logger log) {
        // In reality: send email/push notification to supervisor
        log.info("Notifying supervisor about new record: " + recordId);
    }

    private boolean runBackgroundCheck(String nationalId, String nationality, Logger log) {
        // In reality: call external API (Interpol check, work permit validation, etc.)
        log.info("Running background check for nationalId: " + nationalId);
        return true; // Mock: always passes
    }

    private void updateWorkerVerificationStatus(String workerId, boolean passed, Logger log) {
        // In reality: update CosmosDB document via SDK
        String status = passed ? "ACTIVE" : "SUSPENDED";
        log.info("Updating worker " + workerId + " to status: " + status);
    }

    private void indexWorkerInSearch(String workerId, String workerJson, Logger log) {
        // In reality: call Azure Cognitive Search REST API to index the document
        log.info("Indexing worker in search: " + workerId);
    }

    private String recomputeSeasonStats(String workerId, String season, Logger log) {
        log.info("Recomputing season stats for worker=" + workerId + " season=" + season);
        return String.format("{\"id\":\"%s-%s\", \"workerId\":\"%s\", \"season\":\"%s\"}",
                workerId, season, workerId, season);
    }
}
