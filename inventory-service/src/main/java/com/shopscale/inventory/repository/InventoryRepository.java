package com.shopscale.inventory.repository;

import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.dto.InventoryResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface InventoryRepository extends JpaRepository<InventoryEntity, String> {

    @Query("SELECT new com.shopscale.inventory.dto.InventoryResponseDto(i.sku, i.stock) FROM InventoryEntity i WHERE i.sku = :sku")
    Optional<InventoryResponseDto> findDtoBySku(@Param("sku") String sku);

    @Query("SELECT new com.shopscale.inventory.dto.InventoryResponseDto(i.sku, i.stock) FROM InventoryEntity i ORDER BY i.sku")
    List<InventoryResponseDto> findAllDtos();
}
