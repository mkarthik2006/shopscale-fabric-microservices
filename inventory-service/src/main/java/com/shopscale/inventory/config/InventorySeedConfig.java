package com.shopscale.inventory.config;

import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventorySeedConfig {

    @Bean
    CommandLineRunner seedInventory(InventoryRepository inventoryRepository) {
        return args -> {
            seedIfMissing(inventoryRepository, "P1", 100);
            seedIfMissing(inventoryRepository, "P2", 100);
            seedIfMissing(inventoryRepository, "P3", 100);
        };
    }

    private void seedIfMissing(InventoryRepository repository, String sku, int stock) {
        if (repository.existsById(sku)) {
            return;
        }
        InventoryEntity entity = new InventoryEntity();
        entity.setSku(sku);
        entity.setStock(stock);
        repository.save(entity);
    }
}
