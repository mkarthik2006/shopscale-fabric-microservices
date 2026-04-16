package com.shopscale.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.shopscale.product.model.Product;
import com.shopscale.product.dto.ProductDTO;
import com.shopscale.product.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  // ✅ USE DTO (Optimized API Response)
  @GetMapping
  public List<ProductDTO> all() {
    return service.getAllDTO();
  }

  // ✅ KEEP ENTITY FOR SINGLE FETCH (optional but safe)
  @GetMapping("/{id}")
  public Product one(@PathVariable String id) {
    return service.getById(id);
  }

  // ✅ KEEP CREATE
  @PostMapping
  public Product create(@RequestBody Product product) {
    return service.create(product);
  }

  // ✅ KEEP UPDATE
  @PutMapping("/{id}")
  public Product update(@PathVariable String id, @RequestBody Product req) {
    return service.update(id, req);
  }

  // ✅ ADMIN DELETE (SECURE)
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}