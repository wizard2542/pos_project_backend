package com.pos.backend.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCompletedEvent(
        Long transactionId,
        Long orderId,
        String orderNumber,
        String paymentMethod,
        BigDecimal amount,
        BigDecimal changeAmount,
        String status,
        LocalDateTime createdAt
) {}
