package com.shopscale.order.controller;

import com.shopscale.order.dto.OrderRequestDto;
import com.shopscale.order.dto.OrderResponseDto;
import com.shopscale.order.model.OrderEntity;
import com.shopscale.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService service;
    
    public OrderController(OrderService service) { 
        this.service = service; 
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto dto,
                                                        @AuthenticationPrincipal Jwt jwt) {
        dto.setUserId(resolvePrincipalUserId(jwt));
        dto.setUserEmail(resolvePrincipalEmail(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.placeOrder(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable UUID id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        String principalUserId = resolvePrincipalUserId(jwt);
        OrderEntity saved = service.getById(id);
        if (!principalUserId.equals(saved.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User mismatch between token and order owner");
        }
        return ResponseEntity.ok(new OrderResponseDto(saved.getId(), saved.getStatus(), saved.getUserId()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String principalUserId = resolvePrincipalUserId(jwt);
        List<OrderResponseDto> dtos = service.byUserSummaries(principalUserId);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getByUser(@RequestParam String userId,
                                                            @AuthenticationPrincipal Jwt jwt) {
        String principalUserId = resolvePrincipalUserId(jwt);
        if (!principalUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User mismatch between token and request");
        }
        List<OrderResponseDto> dtos = service.byUserSummaries(userId);
        return ResponseEntity.ok(dtos);
    }

    private String resolvePrincipalUserId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to resolve authenticated user");
    }

    private String resolvePrincipalEmail(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank() && email.contains("@")) {
            return email;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to resolve authenticated email");
    }
}
