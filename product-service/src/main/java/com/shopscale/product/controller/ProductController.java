package com.shopscale.product.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.shopscale.product.model.Product;
import com.shopscale.product.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping
  public List<Product> all() {
    return service.getAll();
  }

  @GetMapping("/{id}")
  public Product one(@PathVariable String id) {
    return service.getById(id);
  }

  @PostMapping
  public Product create(@RequestBody Product product) {
    return service.create(product);
  }

  @PutMapping("/{id}")
  public Product update(@PathVariable String id, @RequestBody Product req) {
    return service.update(id, req);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}