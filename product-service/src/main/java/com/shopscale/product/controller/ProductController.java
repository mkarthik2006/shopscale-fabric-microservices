package com.shopscale.product.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.shopscale.product.dto.ProductDTO;
import com.shopscale.product.dto.ProductUpsertRequestDto;
import com.shopscale.product.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }


  @GetMapping
  public List<ProductDTO> all() {
    return service.getAllDTO();
  }

  @GetMapping("/{id}")
  public ProductDTO one(@PathVariable String id) {
    return service.getByIdDto(id);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ProductDTO create(@Valid @RequestBody ProductUpsertRequestDto request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProductDTO update(@PathVariable String id, @Valid @RequestBody ProductUpsertRequestDto request) {
    return service.update(id, request);
  }


  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}