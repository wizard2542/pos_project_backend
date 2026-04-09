package com.pos.backend.controller;

import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.model.Menu;
import com.pos.backend.repository.MenuRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuRepository menuRepository;

    @GetMapping
    public ResponseEntity<List<Menu>> getAllMenus() {
        return ResponseEntity.ok(menuRepository.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Menu>> getAvailableMenus() {
        return ResponseEntity.ok(menuRepository.findByAvailable(true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menu> getMenuById(@PathVariable Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu", "id", id));
        return ResponseEntity.ok(menu);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Menu>> getMenusByCategory(@PathVariable String category) {
        return ResponseEntity.ok(menuRepository.findByCategory(category));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Menu>> searchMenus(@RequestParam String name) {
        return ResponseEntity.ok(menuRepository.findByNameContainingIgnoreCase(name));
    }

    @PostMapping
    public ResponseEntity<Menu> createMenu(@Valid @RequestBody Menu menu) {
        Menu saved = menuRepository.save(menu);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Menu> updateMenu(@PathVariable Long id,
                                           @Valid @RequestBody Menu menuDetails) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu", "id", id));

        menu.setName(menuDetails.getName());
        menu.setDescription(menuDetails.getDescription());
        menu.setPrice(menuDetails.getPrice());
        menu.setCategory(menuDetails.getCategory());
        menu.setImageUrl(menuDetails.getImageUrl());
        menu.setAvailable(menuDetails.isAvailable());

        return ResponseEntity.ok(menuRepository.save(menu));
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<Menu> toggleAvailability(@PathVariable Long id,
                                                   @RequestParam boolean available) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu", "id", id));
        menu.setAvailable(available);
        return ResponseEntity.ok(menuRepository.save(menu));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu", "id", id));
        menuRepository.delete(menu);
        return ResponseEntity.noContent().build();
    }
}
