package com.pos.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO used to populate a single row in the Sales Report.
 * Each row represents one completed order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportItemDTO {

    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String employeeName;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
}
