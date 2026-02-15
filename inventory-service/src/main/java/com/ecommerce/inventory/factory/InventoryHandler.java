package com.ecommerce.inventory.factory;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.utils.InventoryStrategyType;


public interface InventoryHandler {


    InventoryResponse getInventory(Long productId);


    void updateInventory(InventoryUpdateRequest request);

    String getType();


    InventoryStrategyType getStrategyType();
}