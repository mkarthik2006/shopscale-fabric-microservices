package com.shopscale.product.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shopscale.product.model.Product;
import com.shopscale.product.repository.ProductRepository;
import com.shopscale.product.dto.ProductDTO;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    // ✅ ENTITY FETCH (INTERNAL USE)
    public List<Product> getAll() {
        log.debug("Fetching all products (entity)");
        return repo.findAll();
    }

    // ✅ DTO FETCH (API OPTIMIZED)
    public List<ProductDTO> getAllDTO() {
        log.debug("Fetching all products (DTO)");

        return repo.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ✅ SINGLE FETCH WITH ERROR HANDLING
    public Product getById(String id) {
        log.debug("Fetching product by id={}", id);

        return repo.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Product not found with id: " + id));
    }

    // ✅ CREATE
    public Product create(Product product) {
        log.info("Creating product sku={} name={}", product.getSku(), product.getName());
        return repo.save(product);
    }

    // ✅ UPDATE
    public Product update(String id, Product req) {
        log.info("Updating product id={}", id);

        Product p = repo.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Product not found with id: " + id));

        p.setName(req.getName());
        p.setSku(req.getSku());
        p.setPrice(req.getPrice());
        p.setStock(req.getStock());
        p.setActive(req.getActive());

        return repo.save(p);
    }

    // ✅ DELETE
    public void delete(String id) {
        log.warn("Deleting product id={}", id);
        repo.deleteById(id);
    }

    // ✅ CENTRALIZED DTO MAPPER
    private ProductDTO mapToDTO(Product p) {
        return new ProductDTO(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getPrice(),
                p.getStock(),
                p.getActive()
        );
    }
}