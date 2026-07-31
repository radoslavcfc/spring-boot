package com.farm.workers.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farm.workers.model.Worker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Azure Service Bus                          ║
 * ║  Azure.Messaging.ServiceBus NuGet                        ║
 * ║  ServiceBusClient / ServiceBusSender  →  same classes!  ║
 * ║  Azure SDK for Java mirrors .NET SDK closely             ║
 * ║                                                          ║
 * ║  .NET:                                                   ║
 * ║  var sender = client.CreateSender("queue");             ║
 * ║  await sender.SendMessageAsync(new ServiceBusMessage(..))║
 * ║                                                          ║
 * ║  Java:                                                   ║
 * ║  senderClient.sendMessage(new ServiceBusMessage(...))   ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceBusService {

    private final ObjectMapper objectMapper;  // Jackson JSON serializer  ≈  System.Text.Json

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.worker-events-queue}")
    private String workerEventsQueue;

    @Value("${azure.servicebus.payroll-topic}")
    private String payrollTopic;

    /**
     * Publish a "WorkerCreated" event to Service Bus queue
     * ≈ sending a message in Azure.Messaging.ServiceBus
     *
     * This triggers downstream processes (background checks, onboarding workflows)
     * ≈ domain event / MassTransit publish in .NET
     */
    public void publishWorkerCreatedEvent(Worker worker) {
        try {
            // Build the event payload
            var event = Map.of(
                "eventType", "WorkerCreated",
                "workerId", worker.getId(),
                "workerName", worker.getFirstName() + " " + worker.getLastName(),
                "nationality", worker.getNationality(),
                "timestamp", Instant.now().toString()
            );

            String messageBody = objectMapper.writeValueAsString(event);

            // Create Service Bus sender (client per queue/topic)
            // In production: use a singleton sender client (inject via @Bean)
            try (ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .sender()
                    .queueName(workerEventsQueue)
                    .buildClient()) {

                ServiceBusMessage message = new ServiceBusMessage(messageBody);

                // Service Bus message properties  ≈  ApplicationProperties in .NET
                message.getApplicationProperties().put("eventType", "WorkerCreated");
                message.getApplicationProperties().put("workerId", worker.getId());

                // Subject  ≈  Subject property in .NET ServiceBusMessage
                message.setSubject("WorkerCreated");

                // Message ID for deduplication  ≈  MessageId in .NET
                message.setMessageId("worker-created-" + worker.getId());

                sender.sendMessage(message);
                log.info("Published WorkerCreated event for worker: {}", worker.getId());
            }

        } catch (Exception e) {
            // Don't fail the main request if event publishing fails
            // ≈ fire-and-forget with error logging
            log.error("Failed to publish WorkerCreated event for worker: {}", worker.getId(), e);
        }
    }

    /**
     * Publish payroll event to a Service Bus TOPIC (pub/sub)
     * Multiple subscribers can receive the same message
     * ≈ Topic/Subscription in Azure Service Bus (same concept in .NET)
     */
    public void publishPayrollEvent(String workerId, String season, java.math.BigDecimal amount) {
        try {
            var event = Map.of(
                "eventType", "PayrollCalculated",
                "workerId", workerId,
                "season", season,
                "totalAmount", amount.toString(),
                "timestamp", Instant.now().toString()
            );

            String messageBody = objectMapper.writeValueAsString(event);

            try (ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .sender()
                    .topicName(payrollTopic)  // Note: topicName instead of queueName
                    .buildClient()) {

                ServiceBusMessage message = new ServiceBusMessage(messageBody);
                message.setSubject("PayrollCalculated");
                sender.sendMessage(message);

                log.info("Published PayrollCalculated event for worker: {} season: {}", workerId, season);
            }

        } catch (Exception e) {
            log.error("Failed to publish PayrollCalculated event", e);
        }
    }
}
