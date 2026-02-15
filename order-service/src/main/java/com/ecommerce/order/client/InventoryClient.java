package com.ecommerce.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;
    
    public Map<String, Object> getInventory(Long productId) {
        log.debug("Fetching inventory for product: {}", productId);
        return webClientBuilder.build()
            .get()
            .uri(inventoryServiceUrl + "/inventory/" + productId)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
    
    public void updateInventory(Map<String, Object> request) {
        log.debug("Updating inventory: {}", request);
        webClientBuilder.build()
            .post()
            .uri(inventoryServiceUrl + "/inventory/update")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }
}
