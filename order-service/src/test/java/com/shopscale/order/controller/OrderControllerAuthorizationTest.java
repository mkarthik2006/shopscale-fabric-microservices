package com.shopscale.order.controller;

import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderControllerAuthorizationTest {

    @Test
    @DisplayName("getById should return 403 when token user mismatches order owner")
    void getById_shouldRejectForeignOrderAccess() {
        OrderService service = mock(OrderService.class);
        OrderController controller = new OrderController(service);
        UUID orderId = UUID.randomUUID();

        OrderEntity ownedByAnotherUser = new OrderEntity();
        ownedByAnotherUser.setId(orderId);
        ownedByAnotherUser.setUserId("owner-user");
        ownedByAnotherUser.setUserEmail("owner@shopscale.dev");
        ownedByAnotherUser.setStatus("PLACED");
        ownedByAnotherUser.setCreatedAt(Instant.now());
        when(service.getById(orderId)).thenReturn(ownedByAnotherUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "different-user")
                .claim("email", "different@shopscale.dev")
                .subject("different-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        assertThatThrownBy(() -> controller.getById(orderId, jwt))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
