package com.shopscale.product.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.shopscale.product.model.Product;
import com.shopscale.product.repository.ProductRepository;

@Service
public class ProductService {

  private final ProductRepository repo;

  public ProductService(ProductRepository repo) {
    this.repo = repo;
  }

  public List<Product> getAll() {
    return repo.findAll();
  }

  public Product getById(String id) {
    return repo.findById(id).orElseThrow();
  }

  public Product create(Product product) {
    return repo.save(product);
  }

  public Product update(String id, Product req) {
    Product p = repo.findById(id).orElseThrow();
    p.setName(req.getName());
    p.setSku(req.getSku());
    p.setPrice(req.getPrice());
    p.setStock(req.getStock());
    p.setActive(req.getActive());
    return repo.save(p);
  }

  public void delete(String id) {
    repo.deleteById(id);
  }
}