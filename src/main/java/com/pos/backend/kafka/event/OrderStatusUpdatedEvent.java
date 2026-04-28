package com.pos.backend.kafka.event;

import java.time.LocalDateTime;

public record OrderStatusUpdatedEvent(
        Long orderId,
        String orderNumber,
        String previousStatus,
        String newStatus,
        LocalDateTime updatedAt
) {}
