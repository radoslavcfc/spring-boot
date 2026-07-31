package com.farm.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.logging.Logger;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  Azure Functions in Java                                  ║
 * ║  ≈ Azure Functions in C# (.NET isolated worker)          ║
 * ║                                                          ║
 * ║  C# Function:                                            ║
 * ║  [Function("ComputePayroll")]                           ║
 * ║  public IActionResult Run(                              ║
 * ║    [HttpTrigger(AuthorizationLevel.Function)] req)      ║
 * ║                                                          ║
 * ║  Java Function:                                          ║
 * ║  @FunctionName("ComputePayroll")                        ║
 * ║  public HttpResponseMessage run(                        ║
 * ║    @HttpTrigger(name="req",...) HttpRequestMessage req) ║
 * ║                                                          ║
 * ║  Very similar! Main differences:                         ║
 * ║  - @FunctionName instead of [Function]                  ║
 * ║  - annotations on parameters (not separate attributes)  ║
 * ║  - Logger via context.getLogger() not ILogger<T>        ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * This file shows ALL four trigger types:
 * 1. HttpTrigger       - called via HTTP
 * 2. QueueTrigger      - triggered by Azure Storage Queue message
 * 3. CosmosDBTrigger   - triggered by CosmosDB change feed
 * 4. ServiceBusTrigger - triggered by Service Bus message
 */
public class FarmWorkerFunctions {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════
    // 1. HTTP TRIGGER
    // ≈ [Function] with [HttpTrigger(AuthorizationLevel.Function)]
    // Called directly via HTTP by our Spring Boot API
    // ═══════════════════════════════════════════════════════════

    /**
     * Compute season payroll summary for a worker.
     * Called by AzureFunctionService.computeSeasonPayroll()
     *
     * GET https://{func-app}.azurewebsites.net/api/ComputeSeasonPayroll
     *      ?workerId=xxx&season=2024-HARVEST&code={api-key}
     */
    @FunctionName("ComputeSeasonPayroll")
    public HttpResponseMessage computeSeasonPayroll(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.FUNCTION,  // Requires ?code= or x-functions-key header
                route = "ComputeSeasonPayroll"            // ≈ [Route] in .NET
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("ComputeSeasonPayroll triggered");

        // Get query params  ≈  request.Query["workerId"] in .NET
        String workerId = request.getQueryParameters().get("workerId");
        String season = request.getQueryParameters().get("season");

        if (workerId == null || season == null) {
            // 400 Bad Request  ≈  return new BadRequestObjectResult("...") in .NET
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("workerId and season are required")
                    .build();
        }

        try {
            // In real implementation: query CosmosDB directly
            // Here we return mock data to show the structure
            Map<String, Object> summary = new HashMap<>();
            summary.put("workerId", workerId);
            summary.put("season", season);
            summary.put("totalDaysWorked", 45);
            summary.put("totalHoursWorked", 360.5);
            summary.put("totalEarnings", 5400.75);

            // 200 OK with JSON body  ≈  return new OkObjectResult(summary)
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(summary))
                    .build();

        } catch (Exception e) {
            logger.severe("Error computing payroll: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error computing payroll")
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 2. QUEUE TRIGGER (Azure Storage Queue)
    // ≈ [QueueTrigger] in .NET - fires when a message arrives
    // Our Spring Boot API posts to this queue via StorageQueueService
    // ═══════════════════════════════════════════════════════════

    /**
     * Process work record validation when Spring Boot enqueues a message.
     *
     * Triggered automatically when StorageQueueService.enqueueWorkRecordCreated()
     * puts a message on the "work-records-created" queue.
     *
     * ≈ C# function with [QueueTrigger("work-records-created")] string message
     */
    @FunctionName("ProcessWorkRecordCreated")
    public void processWorkRecordCreated(
            @QueueTrigger(
                name = "message",
                queueName = "work-records-created",
                connection = "STORAGE_CONNECTION_STRING"  // App setting name  ≈  env var
            ) String messageJson,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("ProcessWorkRecordCreated triggered with message: " + messageJson);

        try {
            // Parse the queue message JSON
            Map<String, String> message = objectMapper.readValue(messageJson, Map.class);
            String recordId = message.get("recordId");
            String workerId = message.get("workerId");

            logger.info(String.format("Processing work record %s for worker %s", recordId, workerId));

            // Business logic:
            // 1. Validate no overlapping shifts for same worker on same date
            // 2. Check farm location exists
            // 3. Send supervisor notification
            // 4. Update record status from DRAFT → SUBMITTED

            // Azure Functions auto-retry on exception (configurable in host.json)
            // ≈ MaxDeliveryCount on Service Bus / RetryPolicy in .NET

            logger.info("Work record processed successfully: " + recordId);

        } catch (Exception e) {
            logger.severe("Failed to process work record: " + e.getMessage());
            // Throwing here triggers retry  ≈  throwing in .NET function triggers retry
            throw new RuntimeException("Processing failed", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 3. COSMOS DB TRIGGER (Change Feed)
    // Fires whenever a document is created or updated in CosmosDB
    // ≈ [CosmosDBTrigger] in .NET - reacts to change feed
    // ═══════════════════════════════════════════════════════════

    /**
     * React to changes in the workers container.
     * When a worker document is created/updated, this fires automatically.
     *
     * Use cases:
     * - Sync worker data to a search index (Azure Cognitive Search)
     * - Trigger background verification checks
     * - Audit logging
     * - Caching invalidation
     *
     * ≈ C# function with [CosmosDBTrigger("farm-workers-db", "workers", ...)]
     */
    @FunctionName("OnWorkerChanged")
    public void onWorkerChanged(
            @CosmosDBTrigger(
                name = "workers",
                databaseName = "farm-workers-db",
                containerName = "workers",
                leaseContainerName = "leases",          // Tracks change feed position
                createLeaseContainerIfNotExists = true, // Auto-create leases container
                connection = "COSMOS_CONNECTION_STRING"
            ) String[] documents,                       // Batch of changed documents
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info(String.format("OnWorkerChanged: %d document(s) changed", documents.length));

        for (String document : documents) {
            try {
                Map<String, Object> worker = objectMapper.readValue(document, Map.class);
                String workerId = (String) worker.get("id");
                String status = (String) worker.get("status");

                logger.info(String.format("Worker changed: id=%s status=%s", workerId, status));

                // Example: if worker becomes INACTIVE, cancel pending shifts
                if ("INACTIVE".equals(status)) {
                    logger.info("Worker deactivated - initiating cleanup for: " + workerId);
                    // cancelPendingShifts(workerId);
                    // notifySupervisors(workerId);
                }

                // Example: sync to search index
                // searchIndexClient.mergeOrUploadDocuments(worker);

            } catch (Exception e) {
                logger.severe("Error processing worker change: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 4. SERVICE BUS TRIGGER
    // Fires when a message arrives on a Service Bus queue/topic
    // ≈ [ServiceBusTrigger] in .NET
    // Our Spring Boot API publishes via ServiceBusService
    // ═══════════════════════════════════════════════════════════

    /**
     * Process payroll calculation event from Service Bus topic.
     * Spring Boot publishes to "payroll-events" topic,
     * this subscription processes payslip generation.
     *
     * ≈ C# function with [ServiceBusTrigger("payroll-events", "payslip-generator", ...)]
     */
    @FunctionName("GeneratePayslipOnPayroll")
    public void generatePayslipOnPayroll(
            @ServiceBusTopicTrigger(
                name = "message",
                topicName = "payroll-events",
                subscriptionName = "payslip-generator",
                connection = "SERVICEBUS_CONNECTION_STRING"
            ) String messageJson,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("GeneratePayslipOnPayroll triggered");

        try {
            Map<String, String> event = objectMapper.readValue(messageJson, Map.class);
            String workerId = event.get("workerId");
            String season = event.get("season");
            String totalAmount = event.get("totalAmount");

            logger.info(String.format(
                "Generating payslip: worker=%s season=%s amount=%s",
                workerId, season, totalAmount));

            // In real implementation:
            // 1. Fetch worker details from CosmosDB
            // 2. Fetch all work records for the season
            // 3. Generate PDF using iText or Apache PDFBox
            // 4. Upload PDF to Azure Blob Storage
            // 5. Update work records: paymentReference = blob URL
            // 6. Send email to worker with payslip link

            logger.info("Payslip generated for worker: " + workerId);

        } catch (Exception e) {
            logger.severe("Failed to generate payslip: " + e.getMessage());
            throw new RuntimeException("Payslip generation failed", e);
        }
    }

    /**
     * Process worker created events from Service Bus queue.
     * Triggered by ServiceBusService.publishWorkerCreatedEvent()
     *
     * Initiates background verification / onboarding workflow.
     */
    @FunctionName("OnWorkerCreated")
    public void onWorkerCreated(
            @ServiceBusQueueTrigger(
                name = "message",
                queueName = "worker-events",
                connection = "SERVICEBUS_CONNECTION_STRING"
            ) String messageJson,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("OnWorkerCreated Function triggered");

        try {
            Map<String, String> event = objectMapper.readValue(messageJson, Map.class);

            if (!"WorkerCreated".equals(event.get("eventType"))) {
                logger.info("Skipping non-WorkerCreated event: " + event.get("eventType"));
                return;
            }

            String workerId = event.get("workerId");
            String nationality = event.get("nationality");

            logger.info("Processing new worker: " + workerId + " nationality: " + nationality);

            // In real implementation:
            // 1. Call external verification API (e.g., identity verification service)
            // 2. Check work permit validity
            // 3. Send welcome email/SMS to worker
            // 4. Create worker record in payroll system
            // 5. Update worker status: PENDING_VERIFICATION → ACTIVE or SUSPENDED

        } catch (Exception e) {
            logger.severe("Error processing WorkerCreated event: " + e.getMessage());
            throw new RuntimeException("OnWorkerCreated failed", e);
        }
    }
}
