package com.shopscale.inventory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.shopscale.inventory", "com.shopscale.common"})
@EnableJpaRepositories(basePackages = "com.shopscale.inventory.repository")
public class InventoryServiceApplication {
  public static void main(String[] args) { SpringApplication.run(InventoryServiceApplication.class, args); }
}
