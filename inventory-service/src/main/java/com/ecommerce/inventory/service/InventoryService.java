package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.utils.InventoryStrategyType;
import com.ecommerce.inventory.factory.InventoryHandler;
import com.ecommerce.inventory.factory.InventoryHandlerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {


    private final InventoryHandlerFactory handlerFactory;


    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId, InventoryStrategyType strategyType) {
        log.info("Fetching inventory for product ID: {} using strategy: {}",
                productId, strategyType);

        try {

            InventoryHandler handler = handlerFactory.getHandler(strategyType);

            InventoryResponse response = handler.getInventory(productId);

            log.info("Successfully retrieved inventory for product {} using {} strategy: {} batches, total quantity: {}",
                    productId, strategyType, response.getBatches().size(), response.getTotalQuantity());

            return response;

        } catch (IllegalArgumentException e) {
            log.error("Failed to fetch inventory for product {} with strategy {}: {}",
                    productId, strategyType, e.getMessage());
            throw e;
        }
    }


    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId, String strategyName) {
        InventoryStrategyType strategyType = InventoryStrategyType.fromString(strategyName);
        return getInventory(productId, strategyType);
    }


    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        log.info("Fetching inventory for product ID: {} using default strategy", productId);
        InventoryStrategyType defaultStrategy = InventoryStrategyType.getDefault();
        return getInventory(productId, defaultStrategy);
    }

    @Transactional
    public void updateInventory(InventoryUpdateRequest request, InventoryStrategyType strategyType) {
        log.info("Updating inventory for product {}: {} units from {} batches using strategy: {}",
                request.getProductId(),
                request.getTotalQuantity(),
                request.getBatchUpdates().size(),
                strategyType);


        if(strategyType==null){
            strategyType = InventoryStrategyType.getDefault();
        }

        try {

            InventoryHandler handler = handlerFactory.getHandler(strategyType);


            handler.updateInventory(request);

            log.info("Successfully updated inventory for product {} using {} strategy",
                    request.getProductId(), strategyType);

        } catch (IllegalStateException e) {
            log.error("Failed to update inventory for product {} using strategy {}: {}",
                    request.getProductId(), strategyType, e.getMessage());

            throw e;
        }
    }



    @Transactional(readOnly = true)
    public boolean hasInventory(Long productId) {
        log.debug("Checking inventory existence for product: {}", productId);

        try {
            InventoryResponse response = getInventory(productId);
            boolean exists = response.getTotalQuantity() > 0;
            log.debug("Product {} has inventory: {}", productId, exists);
            return exists;
        } catch (IllegalArgumentException e) {
            log.debug("Product {} has no inventory", productId);
            return false;
        }
    }


    public String[] getAvailableStrategies() {
        return handlerFactory.getAvailableStrategyNames();
    }
}