package com.ecommerce.inventory.utils;

/**
 * INVENTORY STRATEGY TYPE ENUM
 * =============================
 * Defines available inventory management strategies.
 *
 * Why use an Enum instead of Strings?
 * 1. Type Safety: Compile-time checking prevents typos
 * 2. Documentation: All strategies visible in one place
 * 3. IDE Support: Auto-completion shows available strategies
 * 4. Refactoring: Easy to rename without breaking code
 * 5. Validation: Invalid values caught at compile time
 *
 * Usage:
 * - Use .name() to get String representation ("FIFO", "LIFO")
 * - Use .valueOf() to convert String back to enum
 * - Use in API requests/responses for strategy selection
 *
 * Design Pattern: Type-Safe Enum Pattern
 */
public enum InventoryStrategyType {
    /**
     * First In, First Out
     * Consumes oldest inventory first based on expiry date.
     * Best for: Perishable goods, products with expiration dates
     */
    FIFO("First In First Out - Oldest expiry first"),

    /**
     * Last In, First Out
     * Consumes newest inventory first based on expiry date.
     * Best for: Non-perishable goods, cost optimization scenarios
     */
    LIFO("Last In First Out - Newest expiry first"),

    /**
     * Nearest Location First
     * Consumes inventory from nearest warehouse/location.
     * Best for: Reducing shipping costs, faster delivery
     */
    LOCATION_BASED("Location Based - Nearest warehouse first"),

    /**
     * Priority Based
     * Consumes based on business priority rules.
     * Best for: Complex business requirements, promotions
     */
    PRIORITY("Priority Based - Custom business rules");

    private final String description;

    InventoryStrategyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get default strategy for the system.
     * Can be made configurable via application properties.
     */
    public static InventoryStrategyType getDefault() {
        return FIFO;
    }

    /**
     * Safely convert string to enum with validation.
     *
     * @param strategy strategy name as string
     * @return InventoryStrategyType enum value
     * @throws IllegalArgumentException if invalid strategy
     */
    public static InventoryStrategyType fromString(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return getDefault();
        }

        try {
            return InventoryStrategyType.valueOf(strategy.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid inventory strategy: " + strategy +
                            ". Valid strategies are: " + String.join(", ", getAvailableStrategies())
            );
        }
    }

    /**
     * Get list of all available strategy names.
     * Useful for validation and API documentation.
     */
    public static String[] getAvailableStrategies() {
        InventoryStrategyType[] values = InventoryStrategyType.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }
}