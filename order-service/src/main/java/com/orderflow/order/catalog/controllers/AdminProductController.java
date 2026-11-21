package com.orderflow.order.catalog.controllers;

import com.orderflow.order.catalog.dtos.CatalogDtos.ProductRequest;
import com.orderflow.order.catalog.dtos.CatalogDtos.ProductResponse;
import com.orderflow.order.catalog.services.ProductService;
import com.orderflow.order.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(201, "Product created", productService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Product updated", productService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(200, "Product deleted"));
    }
}
