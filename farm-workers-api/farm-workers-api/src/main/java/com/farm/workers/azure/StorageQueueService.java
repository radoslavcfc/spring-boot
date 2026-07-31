package com.farm.workers.azure;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Azure Storage Queues                       ║
 * ║  Azure.Storage.Queues NuGet  →  azure-storage-queue-*   ║
 * ║  QueueClient.SendMessageAsync →  queueClient.sendMessage ║
 * ║                                                          ║
 * ║  Azure Storage Queues vs Service Bus:                    ║
 * ║  Storage Queue = simple, cheap, large volume (triggers  ║
 * ║                  Azure Functions with QueueTrigger)      ║
 * ║  Service Bus   = enterprise features (sessions, DLQ,    ║
 * ║                  ordered delivery, transactions)         ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQueueService {

    private final ObjectMapper objectMapper;

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;

    @Value("${azure.storage.queues.work-records-created}")
    private String workRecordsQueue;

    @Value("${azure.storage.queues.notifications}")
    private String notificationsQueue;

    /**
     * Enqueue a work record created message
     * This will trigger an Azure Function (QueueTrigger) to:
     * 1. Validate the work record
     * 2. Check for overlapping records
     * 3. Send notification to supervisor
     */
    public void enqueueWorkRecordCreated(String recordId, String workerId) {
        try {
            var message = Map.of(
                "recordId", recordId,
                "workerId", workerId,
                "action", "WORK_RECORD_CREATED",
                "timestamp", java.time.Instant.now().toString()
            );

            sendToQueue(workRecordsQueue, message);
            log.info("Enqueued work record created message: {}", recordId);

        } catch (Exception e) {
            log.error("Failed to enqueue work record message: {}", recordId, e);
        }
    }

    /**
     * Enqueue a worker notification
     * Azure Function will process this and send email/SMS
     */
    public void enqueueNotification(String workerId, String type, String message) {
        try {
            var payload = Map.of(
                "workerId", workerId,
                "notificationType", type,
                "message", message,
                "timestamp", java.time.Instant.now().toString()
            );

            sendToQueue(notificationsQueue, payload);

        } catch (Exception e) {
            log.error("Failed to enqueue notification for worker: {}", workerId, e);
        }
    }

    /**
     * Helper to serialize + send to a queue
     *
     * NOTE: Azure Storage Queue messages must be Base64-encoded
     * (the Azure Functions QueueTrigger expects this by default)
     * ≈ QueueClient.SendMessageAsync(BinaryData.FromString(...)) in .NET
     */
    private void sendToQueue(String queueName, Object payload) throws Exception {
        QueueClient queueClient = new QueueClientBuilder()
                .connectionString(storageConnectionString)
                .queueName(queueName)
                .buildClient();

        // Ensure queue exists (idempotent)
        queueClient.createIfNotExists();

        String json = objectMapper.writeValueAsString(payload);

        // Base64 encode - Azure Functions QueueTrigger decodes automatically
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        queueClient.sendMessage(encoded);
    }
}
