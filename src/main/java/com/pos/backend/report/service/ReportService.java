package com.pos.backend.report.service;

import com.pos.backend.exception.BadRequestException;
import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.model.Order;
import com.pos.backend.model.Transaction;
import com.pos.backend.report.dto.OrderReceiptItemDTO;
import com.pos.backend.report.dto.SalesReportItemDTO;
import com.pos.backend.report.dto.TransactionReportItemDTO;
import com.pos.backend.repository.OrderRepository;
import com.pos.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for generating PDF and XLSX reports using JasperReports.
 *
 * <p>JRXML templates are compiled at first use and cached in memory.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Cache of compiled JasperReport objects keyed by classpath resource path. */
    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    // ─── Sales Report ──────────────────────────────────────────────────────────

    /**
     * Generate a sales report for orders created in the given date range.
     *
     * @param start  start of the period (inclusive)
     * @param end    end of the period (inclusive)
     * @param format "pdf" or "xlsx"
     */
    public byte[] generateSalesReport(LocalDateTime start, LocalDateTime end, String format) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        List<SalesReportItemDTO> items = orders.stream().map(o -> SalesReportItemDTO.builder()
                .orderId(o.getId())
                .orderNumber(o.getOrderNumber())
                .orderDate(o.getCreatedAt())
                .employeeName(o.getEmployee().getName())
                .itemCount(o.getItems().size())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus().name())
                .paymentMethod(o.getTransaction() != null
                        ? o.getTransaction().getPaymentMethod().name()
                        : null)
                .build()
        ).toList();

        BigDecimal totalRevenue = items.stream()
                .map(SalesReportItemDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", start.format(DATE_FMT));
        params.put("endDate", end.format(DATE_FMT));
        params.put("totalOrders", items.size());
        params.put("totalRevenue", totalRevenue);

        JasperPrint print = fillReport("reports/sales_report.jrxml", params,
                new JRBeanCollectionDataSource(items));
        return export(print, format);
    }

    // ─── Order Receipt ─────────────────────────────────────────────────────────

    /**
     * Generate a PDF receipt for a single order.
     *
     * @param orderId the order ID
     */
    public byte[] generateOrderReceipt(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        List<OrderReceiptItemDTO> items = order.getItems().stream()
                .map(item -> {
                    int idx = order.getItems().indexOf(item) + 1;
                    return OrderReceiptItemDTO.builder()
                            .lineNumber(idx)
                            .menuName(item.getMenu().getName())
                            .category(item.getMenu().getCategory().getName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subTotal(item.getSubTotal())
                            .build();
                }).toList();

        Transaction txn = order.getTransaction();
        Map<String, Object> params = new HashMap<>();
        params.put("orderNumber", order.getOrderNumber());
        params.put("orderDate", order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_FMT) : "-");
        params.put("cashierName", order.getEmployee().getName());
        params.put("orderStatus", order.getStatus().name());
        params.put("paymentMethod", txn != null ? txn.getPaymentMethod().name() : "-");
        params.put("totalAmount", order.getTotalAmount());
        params.put("amountPaid", txn != null ? txn.getAmount() : order.getTotalAmount());
        params.put("changeAmount", txn != null ? txn.getChangeAmount() : BigDecimal.ZERO);
        params.put("orderNote", order.getNote() != null ? order.getNote() : "");

        JasperPrint print = fillReport("reports/order_receipt.jrxml", params,
                new JRBeanCollectionDataSource(items));
        return export(print, "pdf");
    }

    // ─── Transaction Report ────────────────────────────────────────────────────

    /**
     * Generate a transaction report for the given date range.
     *
     * @param start  start of the period (inclusive)
     * @param end    end of the period (inclusive)
     * @param format "pdf" or "xlsx"
     */
    public byte[] generateTransactionReport(LocalDateTime start, LocalDateTime end, String format) {
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(start, end);

        List<TransactionReportItemDTO> items = transactions.stream().map(t ->
                TransactionReportItemDTO.builder()
                        .transactionId(t.getId())
                        .orderId(t.getOrder().getId())
                        .orderNumber(t.getOrder().getOrderNumber())
                        .transactionDate(t.getCreatedAt())
                        .paymentMethod(t.getPaymentMethod().name())
                        .amount(t.getAmount())
                        .changeAmount(t.getChangeAmount())
                        .status(t.getStatus().name())
                        .cashierName(t.getOrder().getEmployee().getName())
                        .build()
        ).toList();

        BigDecimal totalRevenue = items.stream()
                .map(TransactionReportItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", start.format(DATE_FMT));
        params.put("endDate", end.format(DATE_FMT));
        params.put("totalTransactions", items.size());
        params.put("totalRevenue", totalRevenue);

        JasperPrint print = fillReport("reports/transaction_report.jrxml", params,
                new JRBeanCollectionDataSource(items));
        return export(print, format);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private JasperPrint fillReport(String jrxmlClasspath, Map<String, Object> params,
                                   JRDataSource dataSource) {
        try {
            JasperReport compiled = reportCache.computeIfAbsent(jrxmlClasspath, path -> {
                try (InputStream is = new ClassPathResource(path).getInputStream()) {
                    return JasperCompileManager.compileReport(is);
                } catch (IOException | JRException e) {
                    throw new ReportGenerationException("Failed to compile report template: " + e.getMessage(), e);
                }
            });
            return JasperFillManager.fillReport(compiled, params, dataSource);
        } catch (JRException e) {
            throw new ReportGenerationException("Failed to fill report: " + e.getMessage(), e);
        }
    }

    private byte[] export(JasperPrint print, String format) {
        return switch (format.toLowerCase()) {
            case "pdf"  -> exportPdf(print);
            case "xlsx" -> exportXlsx(print);
            default     -> throw new BadRequestException("Unsupported report format: " + format
                    + ". Supported formats: pdf, xlsx");
        };
    }

    private byte[] exportPdf(JasperPrint print) {
        try {
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new ReportGenerationException("Failed to export report to PDF: " + e.getMessage(), e);
        }
    }

    private byte[] exportXlsx(JasperPrint print) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));

            SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
            config.setOnePagePerSheet(false);
            config.setRemoveEmptySpaceBetweenRows(true);
            config.setDetectCellType(true);
            exporter.setConfiguration(config);

            exporter.exportReport();
            return out.toByteArray();
        } catch (JRException | IOException e) {
            throw new ReportGenerationException("Failed to export report to XLSX: " + e.getMessage(), e);
        }
    }

    // ─── Inner exception ───────────────────────────────────────────────────────

    public static class ReportGenerationException extends RuntimeException {
        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
