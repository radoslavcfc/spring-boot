package com.example.functions.kafka;

import com.example.functions.common.Env;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Lightweight Kafka producer to Azure Event Hubs (Kafka API).
 * For production, set EVENTHUBS_USE_SASL=true and provide connection string;
 * the SASL JAAS config below targets the Event Hubs Kafka endpoint.
 */
public class KafkaSender implements AutoCloseable {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    public KafkaSender() {
        Properties props = new Properties();
        props.put("bootstrap.servers", Env.get("EVENTHUBS_BOOTSTRAP"));
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        props.put("acks", "all");
        props.put("client.id", "func-http-producer");

        if ("true".equalsIgnoreCase(Env.get("EVENTHUBS_USE_SASL", "true"))) {
            String connStr = Env.get("EVENTHUBS_CONNECTION_STRING");
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"$ConnectionString\" password=\"" + connStr + "\";");
        }
        this.producer = new KafkaProducer<>(props);
        this.topic = Env.get("EVENTHUBS_TOPIC", "orders");
    }

    public void send(String key, String json) throws Exception {
        producer.send(new ProducerRecord<>(topic, key, json)).get();
    }

    @Override
    public void close() { producer.close(); }
}
