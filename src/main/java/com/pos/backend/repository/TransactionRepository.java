package com.pos.backend.repository;

import com.pos.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByOrderId(Long orderId);

    List<Transaction> findByPaymentMethod(Transaction.PaymentMethod paymentMethod);

    List<Transaction> findByStatus(Transaction.Status status);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
