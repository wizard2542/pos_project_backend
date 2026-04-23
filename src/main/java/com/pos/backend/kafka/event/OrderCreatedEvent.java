package com.pos.backend.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        Long employeeId,
        String employeeName,
        List<OrderItemDetail> items,
        BigDecimal totalAmount,
        String note,
        LocalDateTime createdAt
) {
    public record OrderItemDetail(
            Long menuId,
            String menuName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
