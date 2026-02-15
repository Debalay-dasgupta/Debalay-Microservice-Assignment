package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//quantity deduction

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdate {
    @NotNull(message = "Batch ID is required")
    private Long batchId;
    
    @NotNull(message = "Quantity deducted is required")
    @Min(value = 1, message = "Quantity deducted must be at least 1")
    private Integer quantityDeducted;
}
