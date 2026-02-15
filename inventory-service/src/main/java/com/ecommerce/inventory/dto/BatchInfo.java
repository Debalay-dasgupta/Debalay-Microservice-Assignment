package com.ecommerce.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

// DTO for  a single inventory batch in API responses

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchInfo {
    private Long batchId;
    private Integer quantity;
    private LocalDate expiryDate;
}
