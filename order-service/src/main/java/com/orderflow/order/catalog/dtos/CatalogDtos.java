package com.orderflow.order.catalog.dtos;

import com.orderflow.order.catalog.entities.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CatalogDtos {

    public record ProductRequest(
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin(value = "0.01") BigDecimal price,
            @Min(0) int stock
    ) {}

    public record ProductResponse(
            Long id, String name, String description, BigDecimal price, int stock
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getDescription(),
                    p.getPrice(), p.getStock());
        }
    }
}
