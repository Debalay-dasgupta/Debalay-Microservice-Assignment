package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;

    private Map<String, Object> mockInventoryResponse;

    @BeforeEach
    void setUp() {

        mockInventoryResponse = new HashMap<>();
        mockInventoryResponse.put("productId", 1001L);
        mockInventoryResponse.put("productName", "Laptop");
        mockInventoryResponse.put("totalQuantity", 50);

        List<Map<String, Object>> batches = new ArrayList<>();
        Map<String, Object> batch1 = new HashMap<>();
        batch1.put("batchId", 1L);
        batch1.put("quantity", 30);
        batch1.put("expiryDate", "2026-06-25");
        batches.add(batch1);

        Map<String, Object> batch2 = new HashMap<>();
        batch2.put("batchId", 2L);
        batch2.put("quantity", 20);
        batch2.put("expiryDate", "2026-03-15");
        batches.add(batch2);

        mockInventoryResponse.put("batches", batches);
    }

    @Test
    @DisplayName("Should place order successfully when inventory available")
    void testPlaceOrder_Success() {

        OrderRequest request = new OrderRequest(1001L, 10);

        when(inventoryClient.getInventory(1001L)).thenReturn(mockInventoryResponse);
        doNothing().when(inventoryClient).updateInventory(any());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L); // Simulate ID assignment
            return order;
        });


        OrderResponse response = orderService.placeOrder(request);


        assertNotNull(response);
        assertEquals(1001L, response.getProductId());
        assertEquals("Laptop", response.getProductName());
        assertEquals(10, response.getQuantity());
        assertEquals("PLACED", response.getStatus());
        assertNotNull(response.getReservedFromBatchIds());

        verify(inventoryClient, times(1)).getInventory(1001L);
        verify(inventoryClient, times(1)).updateInventory(any());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient inventory")
    void testPlaceOrder_InsufficientInventory() {

        OrderRequest request = new OrderRequest(1001L, 100);

        mockInventoryResponse.put("totalQuantity", 50); // Less than requested
        when(inventoryClient.getInventory(1001L)).thenReturn(mockInventoryResponse);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.placeOrder(request)
        );

        assertTrue(exception.getMessage().contains("Insufficient inventory"));
        verify(inventoryClient, times(1)).getInventory(1001L);
        verify(inventoryClient, never()).updateInventory(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle inventory service unavailability")
    void testPlaceOrder_InventoryServiceUnavailable() {

        OrderRequest request = new OrderRequest(1001L, 10);

        when(inventoryClient.getInventory(anyLong()))
                .thenThrow(new RuntimeException("Inventory service unavailable"));


        assertThrows(RuntimeException.class, () -> orderService.placeOrder(request));

        verify(inventoryClient, times(1)).getInventory(1001L);
        verify(inventoryClient, never()).updateInventory(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle product not found in inventory")
    void testPlaceOrder_ProductNotFound() {

        OrderRequest request = new OrderRequest(9999L, 10);

        when(inventoryClient.getInventory(9999L))
                .thenThrow(new RuntimeException("Product not found"));


        assertThrows(RuntimeException.class, () -> orderService.placeOrder(request));

        verify(inventoryClient, times(1)).getInventory(9999L);
        verify(orderRepository, never()).save(any());
    }

}