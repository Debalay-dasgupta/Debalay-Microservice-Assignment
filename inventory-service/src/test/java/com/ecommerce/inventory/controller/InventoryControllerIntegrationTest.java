package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.BatchUpdate;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.utils.InventoryStrategyType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Inventory Controller Integration Tests - Strategy Pattern")
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET INVENTORY TESTS ====================

    @Test
    @DisplayName("GET /inventory/{productId} should return inventory with default FIFO strategy")
    void testGetInventory_DefaultStrategy() throws Exception {
        mockMvc.perform(get("/inventory/1001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(1001))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.batches").isArray())
                .andExpect(jsonPath("$.batches", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.batches[0].batchId").exists())
                .andExpect(jsonPath("$.batches[0].quantity").exists())
                .andExpect(jsonPath("$.batches[0].expiryDate").exists())
                .andExpect(jsonPath("$.totalQuantity").isNumber());
    }

    @Test
    @DisplayName("GET /inventory/{productId}?strategy=FIFO should return FIFO sorted inventory")
    void testGetInventory_WithFifoStrategy() throws Exception {
        mockMvc.perform(get("/inventory/1001?strategy=FIFO"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(1001))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.batches").isArray())
                .andExpect(jsonPath("$.totalQuantity").value(greaterThan(0)));
    }

    @Test
    @DisplayName("GET /inventory/{productId}?strategy=LIFO should return LIFO sorted inventory")
    void testGetInventory_WithLifoStrategy() throws Exception {
        // Product 1005 (Smartwatch) has 3 batches with different expiry dates
        mockMvc.perform(get("/inventory/1005?strategy=LIFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1005))
                .andExpect(jsonPath("$.productName").value("Smartwatch"))
                .andExpect(jsonPath("$.batches", hasSize(3)))
                // LIFO: Latest expiry should be first (2026-05-30)
                .andExpect(jsonPath("$.batches[0].expiryDate").value("2026-05-30"));
    }


    @Test
    @DisplayName("GET /inventory/{productId} should return 404 for non-existent product")
    void testGetInventory_ProductNotFound() throws Exception {
        mockMvc.perform(get("/inventory/9999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(containsString("Product not found")));
    }


    // ==================== UPDATE INVENTORY TESTS ====================

    @Test
    @DisplayName("POST /inventory/update should update inventory with default strategy")
    void testUpdateInventory_DefaultStrategy() throws Exception {
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(1L, 5)
        );
        InventoryUpdateRequest request = new InventoryUpdateRequest(1001L, 5, batchUpdates,InventoryStrategyType.getDefault());

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("updated successfully")))
                .andExpect(jsonPath("$.message").value(containsString("FIFO")));

        // Verify inventory was actually updated
        mockMvc.perform(get("/inventory/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batches[0].quantity").value(63)); // 68 - 5 = 63
    }

    @Test
    @DisplayName("POST /inventory/update with FIFO strategy should update correctly")
    void testUpdateInventory_WithFifoStrategy() throws Exception {
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(2L, 3) // Deduct from batch 2 (Smartwatch)
        );
        InventoryUpdateRequest request = new InventoryUpdateRequest(
                1005L, 3, batchUpdates, InventoryStrategyType.FIFO
        );

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("FIFO")));
    }

    @Test
    @DisplayName("POST /inventory/update with LIFO strategy should update correctly")
    void testUpdateInventory_WithLifoStrategy() throws Exception {
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(5L, 2) // Deduct from batch 5 (Smartwatch)
        );
        InventoryUpdateRequest request = new InventoryUpdateRequest(
                1005L, 2, batchUpdates, InventoryStrategyType.LIFO
        );

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("LIFO")));
    }

    @Test
    @DisplayName("POST /inventory/update should return 400 for invalid request (missing fields)")
    void testUpdateInventory_InvalidRequest() throws Exception {
        String invalidJson = "{\"totalQuantity\": 10, \"batchUpdates\": []}";

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /inventory/update should return 400 when insufficient stock")
    void testUpdateInventory_InsufficientStock() throws Exception {
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(1L, 1000) // More than available
        );
        InventoryUpdateRequest request = new InventoryUpdateRequest(1001L, 1000, batchUpdates,InventoryStrategyType.getDefault());

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(containsString("Insufficient")));
    }

    @Test
    @DisplayName("POST /inventory/update should return 400 for non-existent batch")
    void testUpdateInventory_BatchNotFound() throws Exception {
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(9999L, 10) // Non-existent batch
        );
        InventoryUpdateRequest request = new InventoryUpdateRequest(1001L, 10, batchUpdates,InventoryStrategyType.getDefault());

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ==================== GET STRATEGIES ENDPOINT TESTS ====================

    @Test
    @DisplayName("GET /inventory/strategies should return all available strategies")
    void testGetAvailableStrategies() throws Exception {
        mockMvc.perform(get("/inventory/strategies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.strategies").isArray())
                .andExpect(jsonPath("$.strategies", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.strategies", hasItem("FIFO")))
                .andExpect(jsonPath("$.strategies", hasItem("LIFO")))
                .andExpect(jsonPath("$.defaultStrategy").value("FIFO"));
    }

    @Test
    @DisplayName("POST with null strategy should use default")
    void testUpdateInventory_NullStrategy() throws Exception {
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(4L, 1)
        );
        InventoryUpdateRequest request = new InventoryUpdateRequest(1003L, 1, batchUpdates, null);

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("FIFO")));
    }

}