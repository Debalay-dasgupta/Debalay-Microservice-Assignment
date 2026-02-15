package com.ecommerce.inventory.factory;

import com.ecommerce.inventory.dto.BatchInfo;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.utils.InventoryStrategyType;
import com.ecommerce.inventory.model.InventoryBatch;
import com.ecommerce.inventory.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
@Slf4j
public class FifoInventoryHandler implements InventoryHandler {

    private final InventoryBatchRepository repository;

    @Override
    public InventoryResponse getInventory(Long productId) {
        log.debug("Fetching FIFO inventory for product: {}", productId);

        // Fetch batches sorted by expiry date ASCENDING (oldest first)
        List<InventoryBatch> batches = repository.findByProductIdOrderByExpiryDateAsc(productId);

        if (batches.isEmpty()) {
            log.warn("No inventory found for product: {}", productId);
            throw new IllegalArgumentException("No inventory found for product: " + productId);
        }

        String productName = batches.get(0).getProductName();

        int totalQuantity = batches.stream()
                .mapToInt(InventoryBatch::getQuantity)
                .sum();

        List<BatchInfo> batchInfos = batches.stream()
                .map(batch -> new BatchInfo(
                        batch.getBatchId(),
                        batch.getQuantity(),
                        batch.getExpiryDate()
                ))
                .collect(Collectors.toList());

        log.debug("Found {} batches with total quantity {} for product {} (FIFO order)",
                batches.size(), totalQuantity, productId);

        return new InventoryResponse(productId, productName, batchInfos, totalQuantity);
    }

    @Override
    public void updateInventory(InventoryUpdateRequest request) {
        log.debug("Updating FIFO inventory for product: {}, total quantity: {}",
                request.getProductId(), request.getTotalQuantity());

        request.getBatchUpdates().forEach(batchUpdate -> {
            Long batchId = batchUpdate.getBatchId();
            Integer quantityDeducted = batchUpdate.getQuantityDeducted();

            InventoryBatch batch = repository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Batch not found: " + batchId));

            if (batch.getQuantity() < quantityDeducted) {
                log.error("Insufficient quantity in batch {}. Available: {}, Required: {}",
                        batchId, batch.getQuantity(), quantityDeducted);
                throw new IllegalStateException(
                        "Insufficient quantity in batch " + batchId);
            }

            int newQuantity = batch.getQuantity() - quantityDeducted;
            batch.setQuantity(newQuantity);
            repository.save(batch);

            log.debug("Updated batch {} (FIFO): deducted {}, new quantity: {}",
                    batchId, quantityDeducted, newQuantity);
        });

        log.info("Successfully updated inventory for product: {} using FIFO strategy",
                request.getProductId());
    }

    @Override
    public String getType() {
        return InventoryStrategyType.FIFO.name();
    }

    @Override
    public InventoryStrategyType getStrategyType() {
        return InventoryStrategyType.FIFO;
    }
}