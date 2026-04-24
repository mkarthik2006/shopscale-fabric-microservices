package com.shopscale.inventory.controller;

import com.shopscale.inventory.dto.InventoryResponseDto;
import com.shopscale.inventory.service.InventoryQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryQueryService inventoryQueryService;

    public InventoryController(InventoryQueryService inventoryQueryService) {
        this.inventoryQueryService = inventoryQueryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDto>> all() {
        return ResponseEntity.ok(inventoryQueryService.findAll());
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponseDto> bySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryQueryService.findBySku(sku));
    }
}
