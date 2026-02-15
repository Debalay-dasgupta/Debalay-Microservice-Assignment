package com.ecommerce.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * INVENTORY BATCH ENTITY
 * =======================
 * JPA Entity representing a product batch in inventory.
 * 
 * Business Context:
 * - Each product can have multiple batches with different expiry dates
 * - Batches are consumed in FIFO order (First Expired, First Out)
 * - This enables tracking of product freshness and reducing waste
 * 
 * Design Pattern: Entity/Model in MVC architecture
 * 
 * Annotations Explained:
 * @Entity - Marks this as a JPA entity mapped to database table
 * @Table - Specifies the table name in database
 * @Data - Lombok: Generates getters, setters, toString, equals, hashCode
 * @NoArgsConstructor - Lombok: Generates no-args constructor (required by JPA)
 * @AllArgsConstructor - Lombok: Generates constructor with all fields
 * @Id - Marks the primary key field
 */
@Entity
@Table(name = "inventory_batch")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBatch {
    
    /**
     * Unique identifier for this batch
     * Not auto-generated as IDs are pre-assigned in CSV data
     */
    @Id
    private Long batchId;
    
    /**
     * Product identifier - multiple batches can have same product_id
     * Used to group batches by product for inventory queries
     */
    @Column(nullable = false)
    private Long productId;
    
    /**
     * Product name for display purposes
     * Denormalized for performance (avoids joins with product catalog)
     */
    @Column(nullable = false)
    private String productName;
    
    /**
     * Current quantity available in this batch
     * Updated when orders are placed
     * Must be >= 0 (enforced at business logic layer)
     */
    @Column(nullable = false)
    private Integer quantity;
    
    /**
     * Expiry date of this batch
     * Used to sort batches (oldest expiry first) for FIFO consumption
     */
    @Column(nullable = false)
    private LocalDate expiryDate;
}
