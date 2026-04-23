package com.pos.backend.controller;

import com.pos.backend.exception.DuplicateResourceException;
import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.model.MenuCategory;
import com.pos.backend.repository.MenuCategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-categories")
@RequiredArgsConstructor
public class MenuCategoryController {

    private final MenuCategoryRepository menuCategoryRepository;

    @GetMapping
    public ResponseEntity<List<MenuCategory>> getAllCategories() {
        return ResponseEntity.ok(menuCategoryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuCategory> getCategoryById(@PathVariable Long id) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", id));
        return ResponseEntity.ok(category);
    }

    @PostMapping
    public ResponseEntity<MenuCategory> createCategory(@Valid @RequestBody MenuCategory category) {
        if (menuCategoryRepository.existsByName(category.getName())) {
            throw new DuplicateResourceException("MenuCategory", "name", category.getName());
        }
        return new ResponseEntity<>(menuCategoryRepository.save(category), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuCategory> updateCategory(@PathVariable Long id,
                                                       @Valid @RequestBody MenuCategory categoryDetails) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", id));

        if (!category.getName().equals(categoryDetails.getName())
                && menuCategoryRepository.existsByName(categoryDetails.getName())) {
            throw new DuplicateResourceException("MenuCategory", "name", categoryDetails.getName());
        }

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        return ResponseEntity.ok(menuCategoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", id));
        menuCategoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }
}
