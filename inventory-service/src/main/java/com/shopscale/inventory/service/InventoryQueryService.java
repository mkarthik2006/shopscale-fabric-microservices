package com.shopscale.inventory.service;

import com.shopscale.common.exception.ResourceNotFoundException;
import com.shopscale.inventory.dto.InventoryResponseDto;
import com.shopscale.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;

    public InventoryQueryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryResponseDto> findAll() {
        return inventoryRepository.findAllDtos();
    }

    public InventoryResponseDto findBySku(String sku) {
        return inventoryRepository.findDtoBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for sku: " + sku));
    }
}
