package com.pos.backend.report.controller;

import com.pos.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller that exposes JasperReports-generated report endpoints.
 *
 * <p>All endpoints accept a {@code format} parameter (default {@code pdf}).
 * Use {@code format=xlsx} to receive an Excel file instead.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Sales report – lists every order created between {@code start} and {@code end}.
     *
     * <pre>
     * GET /api/reports/sales?start=2024-01-01T00:00:00&end=2024-01-31T23:59:59&format=pdf
     * </pre>
     */
    @GetMapping("/sales")
    public ResponseEntity<byte[]> salesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "pdf") String format) {

        byte[] report = reportService.generateSalesReport(start, end, format);
        return buildResponse(report, "sales_report", format);
    }

    /**
     * Order receipt – PDF for a single order.
     *
     * <pre>
     * GET /api/reports/orders/{id}
     * </pre>
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<byte[]> orderReceipt(@PathVariable Long id) {
        byte[] report = reportService.generateOrderReceipt(id);
        return buildResponse(report, "order_receipt_" + id, "pdf");
    }

    /**
     * Transaction report – lists every transaction created between {@code start} and {@code end}.
     *
     * <pre>
     * GET /api/reports/transactions?start=2024-01-01T00:00:00&end=2024-01-31T23:59:59&format=xlsx
     * </pre>
     */
    @GetMapping("/transactions")
    public ResponseEntity<byte[]> transactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "pdf") String format) {

        byte[] report = reportService.generateTransactionReport(start, end, format);
        return buildResponse(report, "transaction_report", format);
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildResponse(byte[] data, String filenameBase, String format) {
        MediaType mediaType;
        String extension;

        if ("xlsx".equalsIgnoreCase(format)) {
            mediaType  = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            extension  = "xlsx";
        } else {
            mediaType  = MediaType.APPLICATION_PDF;
            extension  = "pdf";
        }

        String filename = filenameBase + "." + extension;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(data.length)
                .body(data);
    }
}
