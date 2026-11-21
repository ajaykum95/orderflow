package com.orderflow.order.catalog.controllers;

import com.orderflow.order.catalog.dtos.CatalogDtos.ProductResponse;
import com.orderflow.order.catalog.services.ProductService;
import com.orderflow.order.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Products fetched", productService.list(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Product fetched", productService.get(id)));
    }
}
