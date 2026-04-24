package com.shopscale.product.service;

import com.shopscale.product.model.Product;
import com.shopscale.product.repository.ProductRepository;
import com.shopscale.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for ProductService (Product Catalogue CRUD)
 * Doc Ref: Week 1 — "Initialize Product Service (MongoDB)"
 * Doc Ref: Page 2 — "Product Service (MongoDB) — Product catalogue CRUD"
 */
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
    @DisplayName("getAll — returns all products from MongoDB")
    void getAll_shouldReturnAllProducts() {
        when(repo.findAll()).thenReturn(List.of(sampleProduct));

        List<Product> result = productService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("P1");
        assertThat(result.get(0).getName()).isEqualTo("Enterprise Widget");
        verify(repo).findAll();
    }

    @Test
    @DisplayName("getAll — returns empty list when no products exist")
    void getAll_shouldReturnEmptyWhenNone() {
        when(repo.findAll()).thenReturn(List.of());

        List<Product> result = productService.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getById — returns product when found")
    void getById_shouldReturnProductWhenFound() {
        when(repo.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        Product result = productService.getById("prod-1");

        assertThat(result.getName()).isEqualTo("Enterprise Widget");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
    }

    @Test
    @DisplayName("getById — throws ResourceNotFoundException when not found")
    void getById_shouldThrowWhenNotFound() {
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create — saves and returns new product")
    void create_shouldSaveProduct() {
        when(repo.save(any(Product.class))).thenReturn(sampleProduct);

        Product result = productService.create(sampleProduct);

        assertThat(result.getSku()).isEqualTo("P1");
        verify(repo).save(sampleProduct);
    }

    @Test
    @DisplayName("update — modifies existing product fields")
    void update_shouldModifyProduct() {
        Product updateReq = new Product();
        updateReq.setName("Updated Widget");
        updateReq.setSku("P1-V2");
        updateReq.setPrice(new BigDecimal("249.99"));
        updateReq.setStock(200);
        updateReq.setActive(false);

        when(repo.findById("prod-1")).thenReturn(Optional.of(sampleProduct));
        when(repo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update("prod-1", updateReq);

        assertThat(result.getName()).isEqualTo("Updated Widget");
        assertThat(result.getSku()).isEqualTo("P1-V2");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("249.99"));
        assertThat(result.getStock()).isEqualTo(200);
        assertThat(result.getActive()).isFalse();
    }

    @Test
    @DisplayName("update — throws when product not found")
    void update_shouldThrowWhenNotFound() {
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update("nonexistent", new Product()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete — calls repository deleteById")
    void delete_shouldCallDeleteById() {
        productService.delete("prod-1");
        verify(repo).deleteById("prod-1");
    }

    // ===================== NEW DTO TESTS (ADDED ONLY) =====================

    @Test
    @DisplayName("getAllDTO — returns mapped ProductDTO list")
    void getAllDTO_shouldReturnMappedDTOs() {
        when(repo.findAll()).thenReturn(List.of(sampleProduct));

        var result = productService.getAllDTO();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("prod-1");
        assertThat(result.get(0).getSku()).isEqualTo("P1");
        assertThat(result.get(0).getName()).isEqualTo("Enterprise Widget");
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(result.get(0).getStock()).isEqualTo(100);
        assertThat(result.get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("getAllDTO — returns empty list when no products exist")
    void getAllDTO_shouldReturnEmptyWhenNone() {
        when(repo.findAll()).thenReturn(List.of());

        var result = productService.getAllDTO();

        assertThat(result).isEmpty();
    }
}