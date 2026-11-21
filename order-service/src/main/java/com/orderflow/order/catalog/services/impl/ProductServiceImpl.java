package com.orderflow.order.catalog.services.impl;

import com.orderflow.order.catalog.dtos.CatalogDtos.ProductRequest;
import com.orderflow.order.catalog.dtos.CatalogDtos.ProductResponse;
import com.orderflow.order.catalog.entities.Product;
import com.orderflow.order.catalog.repositories.ProductRepository;
import com.orderflow.order.catalog.services.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductResponse create(ProductRequest r) {
        Product saved = repository.save(new Product(r.name(), r.description(), r.price(), r.stock()));
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest r) {
        Product p = findActive(id);
        p.update(r.name(), r.description(), r.price(), r.stock());
        return ProductResponse.from(p);   // @Transactional की वजह से अपने-आप save हो जाता है
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findActive(id).deactivate();
    }

    @Override
    public ProductResponse get(Long id) {
        return ProductResponse.from(findActive(id));
    }

    @Override
    public List<ProductResponse> list(int page, int size) {
        return repository.findByActiveTrue(PageRequest.of(page, Math.min(size, 50)))
                .stream().map(ProductResponse::from).toList();
    }

    @Override
    @Transactional
    public void reserveStock(Long productId, int quantity) {
        int updated = repository.decreaseStock(productId, quantity);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Insufficient stock for product " + productId);
        }
    }

    @Override
    @Transactional
    public void releaseStock(Long productId, int quantity) {
        repository.increaseStock(productId, quantity);
    }

    private Product findActive(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + id));
    }
}
