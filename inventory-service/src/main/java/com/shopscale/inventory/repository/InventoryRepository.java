package com.shopscale.inventory.repository;

import com.shopscale.inventory.model.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryEntity, String> {}
