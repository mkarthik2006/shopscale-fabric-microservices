package com.shopscale.inventory.messaging;

import com.shopscale.common.events.OrderPlacedEvent;
import com.shopscale.inventory.model.InventoryEntity;
import com.shopscale.inventory.model.ProcessedEventEntity;
import com.shopscale.inventory.repository.InventoryRepository;
import com.shopscale.inventory.repository.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderPlacedConsumer {

  private final InventoryRepository inventoryRepository;
  private final ProcessedEventRepository processedEventRepository;

  public OrderPlacedConsumer(InventoryRepository inventoryRepository, ProcessedEventRepository processedEventRepository) {
    this.inventoryRepository = inventoryRepository;
    this.processedEventRepository = processedEventRepository;
  }

  @Transactional
  @KafkaListener(topics = "order.placed", groupId = "inventory-group")
  public void consume(OrderPlacedEvent event) {
    if (processedEventRepository.existsById(event.eventId())) return;

    for (OrderPlacedEvent.Item item : event.items()) {
      InventoryEntity inv = inventoryRepository.findById(item.sku()).orElseGet(() -> {
        InventoryEntity e = new InventoryEntity();
        e.setSku(item.sku());
        e.setStock(0);
        return e;
      });
      inv.setStock(inv.getStock() - item.quantity());
      inventoryRepository.save(inv);
    }
    processedEventRepository.save(new ProcessedEventEntity(event.eventId()));
  }
}
