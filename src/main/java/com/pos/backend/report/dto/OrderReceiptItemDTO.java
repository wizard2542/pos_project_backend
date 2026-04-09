package com.pos.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO used to populate a single line-item row in an Order Receipt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReceiptItemDTO {

    private Integer lineNumber;
    private String menuName;
    private String category;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
