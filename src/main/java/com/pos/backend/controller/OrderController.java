package com.pos.backend.controller;

import com.pos.backend.exception.BadRequestException;
import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.model.Employee;
import com.pos.backend.model.Menu;
import com.pos.backend.model.Order;
import com.pos.backend.model.OrderItem;
import com.pos.backend.repository.EmployeeRepository;
import com.pos.backend.repository.MenuRepository;
import com.pos.backend.repository.OrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final MenuRepository menuRepository;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return ResponseEntity.ok(order);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Order>> getOrdersByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(orderRepository.findByEmployeeId(employeeId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.Status status) {
        return ResponseEntity.ok(orderRepository.findByStatus(status));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Order>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(orderRepository.findByCreatedAtBetween(start, end));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.employeeId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .employee(employee)
                .note(request.note())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest itemReq : request.items()) {
            Menu menu = menuRepository.findById(itemReq.menuId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu", "id", itemReq.menuId()));
            if (!menu.isAvailable()) {
                throw new BadRequestException("Menu item '" + menu.getName() + "' is not available");
            }
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .menu(menu)
                    .quantity(itemReq.quantity())
                    .unitPrice(menu.getPrice())
                    .build();
            order.getItems().add(item);
            totalAmount = totalAmount.add(menu.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestParam Order.Status status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        order.setStatus(status);
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        if (order.getStatus() == Order.Status.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed order");
        }
        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        return ResponseEntity.noContent().build();
    }

    private String generateOrderNumber() {
        String candidate;
        do {
            candidate = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }

    public record CreateOrderRequest(
            @NotNull(message = "Employee ID is required") Long employeeId,
            @NotEmpty(message = "Order items cannot be empty") List<OrderItemRequest> items,
            String note
    ) {
        public record OrderItemRequest(
                @NotNull(message = "Menu ID is required") Long menuId,
                @NotNull(message = "Quantity is required") Integer quantity
        ) {}
    }
}
