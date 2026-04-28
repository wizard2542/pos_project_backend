package com.pos.backend.controller;

import com.pos.backend.exception.BadRequestException;
import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.kafka.event.TransactionCompletedEvent;
import com.pos.backend.kafka.producer.KafkaProducerService;
import com.pos.backend.model.Order;
import com.pos.backend.model.Transaction;
import com.pos.backend.repository.OrderRepository;
import com.pos.backend.repository.TransactionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Transaction> getTransactionByOrderId(@PathVariable Long orderId) {
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "orderId", orderId));
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/payment-method/{method}")
    public ResponseEntity<List<Transaction>> getTransactionsByPaymentMethod(
            @PathVariable Transaction.PaymentMethod method) {
        return ResponseEntity.ok(transactionRepository.findByPaymentMethod(method));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Transaction>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(transactionRepository.findByCreatedAtBetween(start, end));
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.orderId()));

        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new BadRequestException("Cannot process payment for a cancelled order");
        }

        if (transactionRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BadRequestException("A transaction already exists for this order");
        }

        if (request.amount().compareTo(order.getTotalAmount()) < 0) {
            throw new BadRequestException("Payment amount is less than the order total");
        }

        BigDecimal changeAmount = request.amount().subtract(order.getTotalAmount());

        Transaction transaction = Transaction.builder()
                .order(order)
                .paymentMethod(request.paymentMethod())
                .amount(request.amount())
                .changeAmount(changeAmount)
                .status(Transaction.Status.SUCCESS)
                .build();

        order.setStatus(Order.Status.COMPLETED);
        orderRepository.save(order);

        Transaction saved = transactionRepository.save(transaction);

        kafkaProducerService.publishTransactionCompleted(new TransactionCompletedEvent(
                saved.getId(),
                order.getId(),
                order.getOrderNumber(),
                saved.getPaymentMethod().name(),
                saved.getAmount(),
                saved.getChangeAmount(),
                saved.getStatus().name(),
                saved.getCreatedAt()));

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    public record CreateTransactionRequest(
            @NotNull(message = "Order ID is required") Long orderId,
            @NotNull(message = "Payment method is required") Transaction.PaymentMethod paymentMethod,
            @NotNull(message = "Amount is required") BigDecimal amount
    ) {}
}
