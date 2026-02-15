package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.utils.InventoryStrategyType;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;


@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Management", description = "APIs for managing product inventory with configurable strategies (FIFO, LIFO, etc.)")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    //Retrieves inventory batches for a product, sorted by specified strategy.

    @GetMapping("/{productId}")
    @Operation(
            summary = "Get inventory for a product",
            description = "Returns all inventory batches for the specified product, sorted by selected strategy (FIFO by default)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved inventory"),
            @ApiResponse(responseCode = "400", description = "Invalid strategy specified"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getInventory(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "strategy", required = false) String strategy) {

        log.info("Received GET request for inventory: productId={}, strategy={}",
                productId, strategy != null ? strategy : "default");

        try {
            InventoryResponse response;

            if (strategy != null && !strategy.trim().isEmpty()) {
                response = inventoryService.getInventory(productId, strategy);
            } else {
                response = inventoryService.getInventory(productId);
            }

            log.info("Successfully processed GET request for product {} with strategy {}",
                    productId, strategy != null ? strategy : "default");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            log.warn("Invalid request for product {}: {}", productId, e.getMessage());
            if (e.getMessage().contains("No inventory found") ||
                    e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)  // ← Make sure this is 404
                        .body(new ErrorResponse("Product not found: " + productId));
            } else {

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(e.getMessage()));
            }

        } catch (Exception e) {
            log.error("Error fetching inventory for product {}: {}",
                    productId, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }


     // Updates inventory after order placement.

    @PostMapping("/update")
    @Operation(
            summary = "Update inventory",
            description = "Updates inventory quantities after order fulfillment. Optionally specify strategy (FIFO by default)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data, strategy, or insufficient stock"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateInventory(
            @Valid @RequestBody InventoryUpdateRequest request) {

        log.info("Received POST request to update inventory: productId={}, quantity={}, strategy={}",
                request.getProductId(), request.getTotalQuantity(),
                request.getStrategy() != null ? request.getStrategy() : "default");

        try {
            // Determine which strategy to use
           InventoryStrategyType strategyToUse;
            if (request.getStrategy() != null && !request.getStrategy().name().trim().isEmpty()) {
                strategyToUse = request.getStrategy(); // Use provided strategy
            } else {
                strategyToUse = null; // Will use default in service
            }

            // Call service with appropriate strategy
            inventoryService.updateInventory(request, Objects.requireNonNullElseGet(strategyToUse, InventoryStrategyType::getDefault));

            // Determine strategy name for response message
            String strategyUsed = (strategyToUse != null)
                    ? strategyToUse.name()
                    : InventoryStrategyType.getDefault().name();

            String message = String.format(
                    "Inventory updated successfully for product %d using strategy %s",
                    request.getProductId(), strategyUsed);

            log.info(message);
            return ResponseEntity.ok(new SuccessResponse(message));

        } catch (IllegalStateException | IllegalArgumentException e) {
            // Business logic error (insufficient stock, batch not found, invalid strategy)
            log.warn("Failed to update inventory: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));

        } catch (Exception e) {
            // Unexpected error
            log.error("Error updating inventory for product {}: {}",
                    request.getProductId(), e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

 //get all strategies
    @GetMapping("/strategies")
    @Operation(
            summary = "Get available inventory strategies",
            description = "Returns a list of all available inventory management strategies"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved strategies")
    public ResponseEntity<?> getAvailableStrategies() {
        log.info("Received GET request for available strategies");

        try {
            String[] strategies = inventoryService.getAvailableStrategies();
            String defaultStrategy = InventoryStrategyType.getDefault().name();

            return ResponseEntity.ok(new StrategiesResponse(strategies, defaultStrategy));

        } catch (Exception e) {
            log.error("Error retrieving strategies: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    // Response wrapper for success messages

    private record SuccessResponse(String message) {}

    // Response wrapper for error messages

    private record ErrorResponse(String error) {}

   //Response wrapper for strategies endpoint

    private record StrategiesResponse(String[] strategies, String defaultStrategy) {}
}