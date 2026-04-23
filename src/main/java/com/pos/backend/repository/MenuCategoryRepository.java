package com.pos.backend.repository;

import com.pos.backend.model.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    Optional<MenuCategory> findByName(String name);

    boolean existsByName(String name);
}
