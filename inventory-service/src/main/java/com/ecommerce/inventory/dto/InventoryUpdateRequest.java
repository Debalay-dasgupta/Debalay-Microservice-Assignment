package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.utils.InventoryStrategyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Request body for inventory update operations.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer totalQuantity;

    @NotNull(message = "Batch updates list is required")
    private List<BatchUpdate> batchUpdates;


    private InventoryStrategyType strategy;


}