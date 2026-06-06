package com.example.shared.events;

import java.time.Instant;
import java.util.UUID;

/** Payload published from HTTP trigger to Kafka topic and consumed by the Kafka trigger. */
public class OrderEvent {
    private UUID correlationId;
    private String customerId;
    private String sku;
    private int quantity;
    private Instant submittedAt;

    public OrderEvent() {}

    public OrderEvent(UUID correlationId, String customerId, String sku, int quantity, Instant submittedAt) {
        this.correlationId = correlationId;
        this.customerId = customerId;
        this.sku = sku;
        this.quantity = quantity;
        this.submittedAt = submittedAt;
    }

    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID v) { this.correlationId = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getSku() { return sku; }
    public void setSku(String v) { this.sku = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant v) { this.submittedAt = v; }
}
