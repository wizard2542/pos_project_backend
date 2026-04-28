package com.pos.backend.controller;

import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.kafka.event.OrderCreatedEvent;
import com.pos.backend.kafka.producer.KafkaProducerService;
import com.pos.backend.model.Menu;
import com.pos.backend.model.MenuCategory;
import com.pos.backend.repository.MenuCategoryRepository;
import com.pos.backend.repository.MenuRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final KafkaProducerService kafkaProducerService;

    @GetMapping
    public ResponseEntity<Page<Menu>> getAllMenus(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Specification<Menu> spec = Specification.where(null);

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (description != null && !description.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
        }
        if (categoryName != null && !categoryName.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("category").get("name")), categoryName.toLowerCase()));
        }
        if (available != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("available"), available));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("imageUrl")), "%" + imageUrl.toLowerCase() + "%"));
        }

        int page = (limit > 0) ? offset / limit : 0;
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        return ResponseEntity.ok(menuRepository.findAll(spec, pageable));
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

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<Menu>> getMenusByCategory(@PathVariable String categoryName) {
        return ResponseEntity.ok(menuRepository.findByCategory_Name(categoryName));
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
        if (menuDetails.getCategory() != null && menuDetails.getCategory().getId() != null) {
            MenuCategory category = menuCategoryRepository.findById(menuDetails.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", "id", menuDetails.getCategory().getId()));
            menu.setCategory(category);
        }
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

    @PostMapping("/mock")
    public ResponseEntity<List<Menu>> seedMockData() {
        List<Menu> mockMenus = List.of(
            Menu.builder().name("Americano").description("Classic black espresso with hot water").price(new BigDecimal("55.00")).category(findOrCreateCategory("Coffee")).available(true).build(),
            Menu.builder().name("Latte").description("Espresso with steamed milk and light foam").price(new BigDecimal("65.00")).category(findOrCreateCategory("Coffee")).available(true).build(),
            Menu.builder().name("Cappuccino").description("Espresso with equal parts steamed milk and foam").price(new BigDecimal("65.00")).category(findOrCreateCategory("Coffee")).available(true).build(),
            Menu.builder().name("Matcha Latte").description("Japanese green tea powder with steamed milk").price(new BigDecimal("75.00")).category(findOrCreateCategory("Tea")).available(true).build(),
            Menu.builder().name("Thai Milk Tea").description("Spiced black tea with sweetened condensed milk").price(new BigDecimal("60.00")).category(findOrCreateCategory("Tea")).available(true).build(),
            Menu.builder().name("Croissant").description("Buttery, flaky French pastry").price(new BigDecimal("55.00")).category(findOrCreateCategory("Bakery")).available(true).build(),
            Menu.builder().name("Blueberry Muffin").description("Moist muffin packed with blueberries").price(new BigDecimal("50.00")).category(findOrCreateCategory("Bakery")).available(true).build(),
            Menu.builder().name("Chocolate Cake").description("Rich dark chocolate layered cake").price(new BigDecimal("80.00")).category(findOrCreateCategory("Dessert")).available(true).build(),
            Menu.builder().name("Mango Smoothie").description("Blended fresh mango with yogurt").price(new BigDecimal("70.00")).category(findOrCreateCategory("Smoothie")).available(true).build(),
            Menu.builder().name("Orange Juice").description("Freshly squeezed orange juice").price(new BigDecimal("60.00")).category(findOrCreateCategory("Juice")).available(true).build()
        );
        List<Menu> saved = menuRepository.saveAll(mockMenus);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    private MenuCategory findOrCreateCategory(String name) {
        return menuCategoryRepository.findByName(name)
                .orElseGet(() -> menuCategoryRepository.save(
                        MenuCategory.builder().name(name).build()));
    }

    @GetMapping("/kafka-test")
    public ResponseEntity<Void> kafkaTest() {
        // Implement Kafka test logic here
        kafkaProducerService.publishOrderCreated(new OrderCreatedEvent(
                999L,
                "TEST-ORDER-999",
                1L,
                "Test Employee",
                List.of(new OrderCreatedEvent.OrderItemDetail(1L, "Test Menu", 2, new BigDecimal("50.00"))),
                new BigDecimal("100.00"),
                "This is a test order",
                LocalDateTime.now()
        ));

        return ResponseEntity.ok().build();
    }

}