package com.orderflow.order.catalog.services;

import com.orderflow.order.catalog.dtos.CatalogDtos.ProductRequest;
import com.orderflow.order.catalog.dtos.CatalogDtos.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductResponse get(Long id);

    List<ProductResponse> list(int page, int size);

    void reserveStock(Long productId, int quantity);

    void releaseStock(Long productId, int quantity);
}
