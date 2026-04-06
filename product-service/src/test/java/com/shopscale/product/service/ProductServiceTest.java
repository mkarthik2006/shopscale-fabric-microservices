package com.shopscale.product.service;

import com.shopscale.product.model.Product;
import com.shopscale.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository repo;
    @InjectMocks private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId("prod-1");
        sampleProduct.setSku("P1");
        sampleProduct.setName("Enterprise Widget");
        sampleProduct.setPrice(new BigDecimal("199.99"));
        sampleProduct.setStock(100);
        sampleProduct.setActive(true);
    }

    @Test
    @DisplayName("getAll - returns all products")
    void getAll_shouldReturnAllProducts() {
        when(repo.findAll()).thenReturn(List.of(sampleProduct));
        List<Product> result = productService.getAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("P1");
    }

    @Test
    @DisplayName("getById - returns product when found")
    void getById_shouldReturnProduct() {
        when(repo.findById("prod-1")).thenReturn(Optional.of(sampleProduct));
        Product result = productService.getById("prod-1");
        assertThat(result.getName()).isEqualTo("Enterprise Widget");
    }

    @Test
    @DisplayName("getById - throws when not found")
    void getById_shouldThrowWhenNotFound() {
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getById("nonexistent"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("create - saves and returns product")
    void create_shouldSaveProduct() {
        when(repo.save(any(Product.class))).thenReturn(sampleProduct);
        Product result = productService.create(sampleProduct);
        assertThat(result.getSku()).isEqualTo("P1");
        verify(repo).save(sampleProduct);
    }

    @Test
    @DisplayName("delete - calls deleteById")
    void delete_shouldCallRepo() {
        productService.delete("prod-1");
        verify(repo).deleteById("prod-1");
    }
}