package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.BatchInfo;
import com.ecommerce.inventory.dto.BatchUpdate;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.utils.InventoryStrategyType;
import com.ecommerce.inventory.factory.InventoryHandler;
import com.ecommerce.inventory.factory.InventoryHandlerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Service Unit Tests - Strategy Pattern")
class InventoryServiceTest {

    @Mock
    private InventoryHandlerFactory handlerFactory;

    @Mock
    private InventoryHandler fifoHandler;

    @Mock
    private InventoryHandler lifoHandler;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryResponse fifoResponse;
    private InventoryResponse lifoResponse;
    private InventoryUpdateRequest sampleUpdateRequest;


    @BeforeEach
    void setUp() {
        // FIFO response: batches sorted oldest first
        List<BatchInfo> fifoBatches = Arrays.asList(
                new BatchInfo(2L, 30, LocalDate.of(2026, 3, 15)),  // Older
                new BatchInfo(1L, 50, LocalDate.of(2026, 6, 25))   // Newer
        );
        fifoResponse = new InventoryResponse(1001L, "Laptop", fifoBatches, 80);

        // LIFO response: batches sorted newest first
        List<BatchInfo> lifoBatches = Arrays.asList(
                new BatchInfo(1L, 50, LocalDate.of(2026, 6, 25)),  // Newer
                new BatchInfo(2L, 30, LocalDate.of(2026, 3, 15))   // Older
        );
        lifoResponse = new InventoryResponse(1001L, "Laptop", lifoBatches, 80);

        // Sample update request
        List<BatchUpdate> batchUpdates = Arrays.asList(
                new BatchUpdate(1L, 10)
        );
        sampleUpdateRequest = new InventoryUpdateRequest(1001L, 10, batchUpdates,InventoryStrategyType.getDefault());
    }

    // ==================== GET INVENTORY TESTS ====================

    @Test
    @DisplayName("Should return FIFO inventory when FIFO strategy specified")
    void testGetInventory_WithFifoStrategy() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.FIFO)).thenReturn(fifoHandler);
        when(fifoHandler.getInventory(1001L)).thenReturn(fifoResponse);

        // Act
        InventoryResponse result = inventoryService.getInventory(1001L, InventoryStrategyType.FIFO);

        // Assert
        assertNotNull(result);
        assertEquals(1001L, result.getProductId());
        assertEquals("Laptop", result.getProductName());
        assertEquals(2, result.getBatches().size());
        // Verify FIFO order: oldest batch first
        assertEquals(LocalDate.of(2026, 3, 15), result.getBatches().get(0).getExpiryDate());
        assertEquals(LocalDate.of(2026, 6, 25), result.getBatches().get(1).getExpiryDate());

        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.FIFO);
        verify(fifoHandler, times(1)).getInventory(1001L);
    }

    @Test
    @DisplayName("Should return LIFO inventory when LIFO strategy specified")
    void testGetInventory_WithLifoStrategy() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.LIFO)).thenReturn(lifoHandler);
        when(lifoHandler.getInventory(1001L)).thenReturn(lifoResponse);

        // Act
        InventoryResponse result = inventoryService.getInventory(1001L, InventoryStrategyType.LIFO);

        // Assert
        assertNotNull(result);
        assertEquals(1001L, result.getProductId());
        assertEquals(2, result.getBatches().size());
        // Verify LIFO order: newest batch first
        assertEquals(LocalDate.of(2026, 6, 25), result.getBatches().get(0).getExpiryDate());
        assertEquals(LocalDate.of(2026, 3, 15), result.getBatches().get(1).getExpiryDate());

        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.LIFO);
        verify(lifoHandler, times(1)).getInventory(1001L);
    }


    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetInventory_ProductNotFound() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.FIFO)).thenReturn(fifoHandler);
        when(fifoHandler.getInventory(9999L))
                .thenThrow(new IllegalArgumentException("No inventory found for product: 9999"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.getInventory(9999L)
        );

        assertTrue(exception.getMessage().contains("No inventory found"));
        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.FIFO);
        verify(fifoHandler, times(1)).getInventory(9999L);
    }

    // ==================== UPDATE INVENTORY TESTS ====================

    @Test
    @DisplayName("Should update inventory using FIFO strategy")
    void testUpdateInventory_WithFifoStrategy() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.FIFO)).thenReturn(fifoHandler);
        doNothing().when(fifoHandler).updateInventory(sampleUpdateRequest);

        // Act
        assertDoesNotThrow(() ->
                inventoryService.updateInventory(sampleUpdateRequest, InventoryStrategyType.FIFO)
        );

        // Verify
        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.FIFO);
        verify(fifoHandler, times(1)).updateInventory(sampleUpdateRequest);
    }

    @Test
    @DisplayName("Should update inventory using LIFO strategy")
    void testUpdateInventory_WithLifoStrategy() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.LIFO)).thenReturn(lifoHandler);
        doNothing().when(lifoHandler).updateInventory(sampleUpdateRequest);

        // Act
        assertDoesNotThrow(() ->
                inventoryService.updateInventory(sampleUpdateRequest, InventoryStrategyType.LIFO)
        );

        // Verify
        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.LIFO);
        verify(lifoHandler, times(1)).updateInventory(sampleUpdateRequest);
    }

    @Test
    @DisplayName("Should use default strategy when no strategy specified in update")
    void testUpdateInventory_WithDefaultStrategy() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.FIFO)).thenReturn(fifoHandler);
        doNothing().when(fifoHandler).updateInventory(sampleUpdateRequest);

        // Act
        assertDoesNotThrow(() -> inventoryService.updateInventory(sampleUpdateRequest,InventoryStrategyType.getDefault()));

        // Verify default (FIFO) was used
        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.FIFO);
        verify(fifoHandler, times(1)).updateInventory(sampleUpdateRequest);
    }


    @Test
    @DisplayName("Should use default strategy in hasInventory when not specified")
    void testHasInventory_DefaultStrategy() {
        // Arrange
        when(handlerFactory.getHandler(InventoryStrategyType.FIFO)).thenReturn(fifoHandler);
        when(fifoHandler.getInventory(1001L)).thenReturn(fifoResponse);

        // Act
        boolean result = inventoryService.hasInventory(1001L);

        // Assert
        assertTrue(result);
        verify(handlerFactory, times(1)).getHandler(InventoryStrategyType.FIFO);
    }

    // ==================== GET AVAILABLE STRATEGIES TEST ====================

    @Test
    @DisplayName("Should return all available strategies")
    void testGetAvailableStrategies() {
        // Arrange
        String[] expectedStrategies = {"FIFO", "LIFO"};
        when(handlerFactory.getAvailableStrategyNames()).thenReturn(expectedStrategies);

        // Act
        String[] result = inventoryService.getAvailableStrategies();

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedStrategies, result);
        verify(handlerFactory, times(1)).getAvailableStrategyNames();
    }
}