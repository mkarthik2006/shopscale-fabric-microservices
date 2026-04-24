package com.shopscale.order.controller;

import com.shopscale.order.dto.OrderRequestDto;
import com.shopscale.order.dto.OrderResponseDto;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerUnitTest {

    @Mock
    private OrderService service;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderController(service);
    }

    @Test
    void createOrder_shouldInjectPrincipalIdentityAndReturnCreated() {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setCurrency("USD");
        dto.setItems(List.of(new OrderRequestDto.ItemDto()));

        OrderResponseDto responseDto = new OrderResponseDto(UUID.randomUUID(), "PLACED", "alice");
        when(service.placeOrder(any(OrderRequestDto.class))).thenReturn(responseDto);

        Jwt jwt = jwt("alice", "alice@shopscale.dev", "subject-1");
        var response = controller.createOrder(dto, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDto);
        assertThat(dto.getUserId()).isEqualTo("alice");
        assertThat(dto.getUserEmail()).isEqualTo("alice@shopscale.dev");
    }

    @Test
    void getById_shouldRejectMismatchedPrincipalUser() {
        UUID orderId = UUID.randomUUID();
        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setUserId("owner");
        entity.setStatus("PLACED");
        when(service.getById(orderId)).thenReturn(entity);

        Jwt jwt = jwt("other-user", "other@shopscale.dev", "sub");
        assertThatThrownBy(() -> controller.getById(orderId, jwt))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void getByUser_shouldRequireMatchingPrincipal() {
        Jwt jwt = jwt("principal-user", "principal@shopscale.dev", "sub");

        assertThatThrownBy(() -> controller.getByUser("different-user", jwt))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void getMyOrders_shouldUseSubjectWhenPreferredUsernameMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "subject-user@shopscale.dev")
                .subject("subject-user")
                .build();
        List<OrderResponseDto> expected = List.of(new OrderResponseDto(UUID.randomUUID(), "PLACED", "subject-user"));
        when(service.byUserSummaries("subject-user")).thenReturn(expected);

        var response = controller.getMyOrders(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(expected);
        verify(service).byUserSummaries("subject-user");
    }

    private Jwt jwt(String preferredUsername, String email, String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(claims -> claims.putAll(Map.of(
                        "preferred_username", preferredUsername,
                        "email", email
                )))
                .subject(subject)
                .build();
    }
}
