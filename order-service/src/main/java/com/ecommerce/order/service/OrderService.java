package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Placing order for product {}, quantity {}", request.getProductId(), request.getQuantity());
        
        Map<String, Object> inventoryData = inventoryClient.getInventory(request.getProductId());
        List<Map<String, Object>> batches = (List<Map<String, Object>>) inventoryData.get("batches");
        Integer totalQuantity = (Integer) inventoryData.get("totalQuantity");
        
        if (totalQuantity < request.getQuantity()) {
            throw new IllegalStateException("Insufficient inventory");
        }
        
        List<Map<String, Object>> batchUpdates = new ArrayList<>();
        List<Long> reservedBatchIds = new ArrayList<>();
        int remaining = request.getQuantity();
        
        for (Map<String, Object> batch : batches) {
            if (remaining <= 0) break;
            Long batchId = ((Number) batch.get("batchId")).longValue();
            Integer available = (Integer) batch.get("quantity");
            int toDeduct = Math.min(remaining, available);
            
            Map<String, Object> update = new HashMap<>();
            update.put("batchId", batchId);
            update.put("quantityDeducted", toDeduct);
            batchUpdates.add(update);
            reservedBatchIds.add(batchId);
            remaining -= toDeduct;
        }
        
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("productId", request.getProductId());
        updateRequest.put("totalQuantity", request.getQuantity());
        updateRequest.put("batchUpdates", batchUpdates);
        
        inventoryClient.updateInventory(updateRequest);
        
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setProductName((String) inventoryData.get("productName"));
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PLACED);
        order.setReservedFromBatchIds(reservedBatchIds.stream()
            .map(String::valueOf).collect(Collectors.joining(",")));
        
        order = orderRepository.save(order);
        
        return new OrderResponse(
            order.getOrderId(),
            order.getProductId(),
            order.getProductName(),
            order.getQuantity(),
            order.getStatus().name(),
            reservedBatchIds,
            "Order placed. Inventory reserved.",
            order.getOrderDate()
        );
    }
}
