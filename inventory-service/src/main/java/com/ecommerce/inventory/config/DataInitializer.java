package com.ecommerce.inventory.config;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final InventoryRepository inventoryRepository;

    @PostConstruct
    public void seed() {
        if (inventoryRepository.count() > 0) {
            log.info("Inventory already seeded; skipping sample data");
            return;
        }

        List<Inventory> items = List.of(
                Inventory.builder().productId("PROD-001").productName("Laptop Dell XPS 15").category("Electronics").availableQuantity(100).reservedQuantity(0).unitPrice(1299.99).build(),
                Inventory.builder().productId("PROD-002").productName("iPhone 15 Pro").category("Electronics").availableQuantity(50).reservedQuantity(0).unitPrice(999.99).build(),
                Inventory.builder().productId("PROD-003").productName("Nike Air Max Shoes").category("Fashion").availableQuantity(200).reservedQuantity(0).unitPrice(129.99).build(),
                Inventory.builder().productId("PROD-004").productName("Samsung Galaxy Watch").category("Electronics").availableQuantity(75).reservedQuantity(0).unitPrice(299.99).build(),
                Inventory.builder().productId("PROD-005").productName("Adidas Running Shirt").category("Fashion").availableQuantity(150).reservedQuantity(0).unitPrice(49.99).build()
        );

        inventoryRepository.saveAll(items);
        log.info("Seeded sample inventory items: {}", items.size());
    }
}
