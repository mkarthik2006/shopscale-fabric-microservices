package com.shopscale.product.controller;

import com.shopscale.product.dto.ProductDTO;
import com.shopscale.product.dto.ProductUpsertRequestDto;
import com.shopscale.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productService);
    }

    @Test
    void all_shouldReturnServiceDtos() {
        ProductDTO dto = new ProductDTO("p1", "SKU-1", "Widget", new BigDecimal("10.00"), 5, true);
        when(productService.getAllDTO()).thenReturn(List.of(dto));

        List<ProductDTO> result = controller.all();

        assertThat(result).containsExactly(dto);
        verify(productService).getAllDTO();
    }

    @Test
    void one_shouldReturnDtoFromService() {
        ProductDTO dto = new ProductDTO("p1", "SKU-1", "Widget", new BigDecimal("10.00"), 5, true);
        when(productService.getByIdDto("p1")).thenReturn(dto);

        ProductDTO result = controller.one("p1");

        assertThat(result).isEqualTo(dto);
        verify(productService).getByIdDto("p1");
    }

    @Test
    void create_shouldDelegateToService() {
        ProductUpsertRequestDto request = buildRequest("SKU-1", "Widget", new BigDecimal("10.00"), 5, true);
        ProductDTO dto = new ProductDTO("p1", "SKU-1", "Widget", new BigDecimal("10.00"), 5, true);
        when(productService.create(request)).thenReturn(dto);

        ProductDTO result = controller.create(request);

        assertThat(result).isEqualTo(dto);
        verify(productService).create(request);
    }

    @Test
    void update_shouldDelegateToService() {
        ProductUpsertRequestDto request = buildRequest("SKU-1", "Widget Updated", new BigDecimal("12.00"), 7, true);
        ProductDTO dto = new ProductDTO("p1", "SKU-1", "Widget Updated", new BigDecimal("12.00"), 7, true);
        when(productService.update("p1", request)).thenReturn(dto);

        ProductDTO result = controller.update("p1", request);

        assertThat(result).isEqualTo(dto);
        verify(productService).update("p1", request);
    }

    @Test
    void delete_shouldDelegateToService() {
        controller.delete("p1");
        verify(productService).delete("p1");
    }

    private ProductUpsertRequestDto buildRequest(String sku, String name, BigDecimal price, int stock, boolean active) {
        ProductUpsertRequestDto request = new ProductUpsertRequestDto();
        request.setSku(sku);
        request.setName(name);
        request.setPrice(price);
        request.setStock(stock);
        request.setActive(active);
        return request;
    }
}
