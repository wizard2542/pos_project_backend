package com.pos.backend.repository;

import com.pos.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByEmployeeId(Long employeeId);

    List<Order> findByStatus(Order.Status status);

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.employee.id = :employeeId AND o.status = :status")
    List<Order> findByEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                          @Param("status") Order.Status status);

    boolean existsByOrderNumber(String orderNumber);
}
