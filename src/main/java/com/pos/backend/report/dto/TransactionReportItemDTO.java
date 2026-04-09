package com.pos.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO used to populate a single row in the Transaction Report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionReportItemDTO {

    private Long transactionId;
    private Long orderId;
    private String orderNumber;
    private LocalDateTime transactionDate;
    private String paymentMethod;
    private BigDecimal amount;
    private BigDecimal changeAmount;
    private String status;
    private String cashierName;
}
