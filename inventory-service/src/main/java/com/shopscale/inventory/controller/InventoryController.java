package com.shopscale.inventory.controller;

import com.shopscale.inventory.dto.InventoryResponseDto;
import com.shopscale.inventory.repository.InventoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDto>> all() {
        return ResponseEntity.ok(inventoryRepository.findAllDtos());
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponseDto> bySku(@PathVariable String sku) {
        return inventoryRepository.findDtoBySku(sku)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
