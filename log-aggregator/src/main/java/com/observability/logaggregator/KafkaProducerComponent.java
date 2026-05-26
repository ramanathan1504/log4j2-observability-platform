package com.observability.logaggregator;

import org.springframework.stereotype.Component;


@Component
public class KafkaProducerComponent {
    public void send(String topic, LogEvent event) {
        // In a real implementation, this would serialize the LogEvent and send it to Kafka.
        // For this scaffold, we'll just print the event to the console.
        System.out.printf("Producing to topic '%s': %s%n", topic, event);
    }
}
