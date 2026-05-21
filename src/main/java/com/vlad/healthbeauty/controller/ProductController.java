package com.vlad.healthbeauty.controller;

import com.vlad.healthbeauty.dto.ProductDTO;
import com.vlad.healthbeauty.model.Product;
import com.vlad.healthbeauty.service.AuditLogService;
import com.vlad.healthbeauty.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final AuditLogService auditLogService;

    public ProductController(ProductService productService, AuditLogService auditLogService) {
        this.productService = productService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Access: Public")
    public List<Product> getAll() {
        return productService.findAll();
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock products", description = "Access: Public")
    public List<Product> getLowStock(@RequestParam(defaultValue = "10") int threshold) {
        return productService.findLowStock(threshold);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id", description = "Access: Authenticated user")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Access: ROLE_ADMIN")
    public ResponseEntity<Product> create(@Valid @RequestBody ProductDTO productDTO) {
        Product saved = productService.save(productDTO);
        auditLogService.log(
                "CREATE",
                "PRODUCT",
                saved.getId(),
                "Created product: " + saved.getName()
        );
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update product", description = "Access: ROLE_ADMIN or ROLE_STAFF")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        try {
            Product updated = productService.update(id, productDTO);
            auditLogService.log(
                    "UPDATE",
                    "PRODUCT",
                    updated.getId(),
                    "Updated product: " + updated.getName()
            );
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/update-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update product stock", description = "Access: ROLE_ADMIN or ROLE_STAFF")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        int newQuantity = body.get("quantity");
        Product updated = productService.updateStock(id, newQuantity);
        auditLogService.log(
                "UPDATE_STOCK",
                "PRODUCT",
                updated.getId(),
                "Updated stock for product: " + updated.getName() + " to " + newQuantity
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Access: ROLE_ADMIN")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        var existing = productService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        productService.deleteById(id);
        auditLogService.log(
                "DELETE",
                "PRODUCT",
                id,
                "Deleted product: " + existing.get().getName()
        );
        return ResponseEntity.noContent().build();
    }
}
