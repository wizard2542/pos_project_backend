package com.pos.backend.repository;

import com.pos.backend.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByCategory(String category);

    List<Menu> findByAvailable(boolean available);

    List<Menu> findByCategoryAndAvailable(String category, boolean available);

    List<Menu> findByNameContainingIgnoreCase(String name);

    List<Menu> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
}
