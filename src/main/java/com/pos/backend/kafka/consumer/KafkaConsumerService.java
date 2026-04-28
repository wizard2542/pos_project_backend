package com.pos.backend.kafka.consumer;

import com.pos.backend.kafka.event.OrderCreatedEvent;
import com.pos.backend.kafka.event.OrderStatusUpdatedEvent;
import com.pos.backend.kafka.event.TransactionCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(
            topics = "${kafka.topic.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, orderNumber={}, totalAmount={}",
                event.orderId(), event.orderNumber(), event.totalAmount());
    }

    @KafkaListener(
            topics = "${kafka.topic.order-status-updated}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("Received OrderStatusUpdatedEvent: orderId={}, orderNumber={}, {} -> {}",
                event.orderId(), event.orderNumber(), event.previousStatus(), event.newStatus());
    }

    @KafkaListener(
            topics = "${kafka.topic.transaction-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Received TransactionCompletedEvent: transactionId={}, orderId={}, paymentMethod={}, amount={}",
                event.transactionId(), event.orderId(), event.paymentMethod(), event.amount());
    }
}
