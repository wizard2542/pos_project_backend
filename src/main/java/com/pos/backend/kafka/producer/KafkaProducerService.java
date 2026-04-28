package com.pos.backend.kafka.producer;

import com.pos.backend.kafka.event.OrderCreatedEvent;
import com.pos.backend.kafka.event.OrderStatusUpdatedEvent;
import com.pos.backend.kafka.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topic.order-status-updated}")
    private String orderStatusUpdatedTopic;

    @Value("${kafka.topic.transaction-completed}")
    private String transactionCompletedTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderCreatedTopic, event.orderNumber(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderCreatedEvent for order {}: {}",
                        event.orderNumber(), ex.getMessage());
            } else {
                log.info("Published OrderCreatedEvent for order {} to partition {}",
                        event.orderNumber(), result.getRecordMetadata().partition());
            }
        });
    }

    public void publishOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderStatusUpdatedTopic, event.orderNumber(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderStatusUpdatedEvent for order {}: {}",
                        event.orderNumber(), ex.getMessage());
            } else {
                log.info("Published OrderStatusUpdatedEvent for order {} -> {} to partition {}",
                        event.orderNumber(), event.newStatus(), result.getRecordMetadata().partition());
            }
        });
    }

    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(transactionCompletedTopic, event.orderNumber(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish TransactionCompletedEvent for order {}: {}",
                        event.orderNumber(), ex.getMessage());
            } else {
                log.info("Published TransactionCompletedEvent for order {} to partition {}",
                        event.orderNumber(), result.getRecordMetadata().partition());
            }
        });
    }
}
