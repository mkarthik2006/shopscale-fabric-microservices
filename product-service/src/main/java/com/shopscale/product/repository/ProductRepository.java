package com.shopscale.product.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.shopscale.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
  Optional<Product> findBySku(String sku);
}