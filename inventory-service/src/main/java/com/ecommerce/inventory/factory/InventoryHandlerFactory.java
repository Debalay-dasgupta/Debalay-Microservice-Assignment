package com.ecommerce.inventory.factory;

import com.ecommerce.inventory.utils.InventoryStrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
public class InventoryHandlerFactory {


    private final Map<InventoryStrategyType, InventoryHandler> handlers;


    public InventoryHandlerFactory(List<InventoryHandler> handlerList) {
        this.handlers = new EnumMap<>(InventoryStrategyType.class);


        handlerList.forEach(handler -> {
            InventoryStrategyType type = handler.getStrategyType();
            //handlers.put(FIFO, FifoInventoryHandler@def456)
            handlers.put(type, handler);
            log.info("Registered inventory handler: {} -> {}",
                    type, handler.getClass().getSimpleName());
        });

        log.info("Initialized InventoryHandlerFactory with {} handlers: {}",
                handlers.size(), handlers.keySet());

        validateAllStrategiesImplemented();
    }

    public InventoryHandler getHandler(InventoryStrategyType strategyType) {
        InventoryHandler handler = handlers.get(strategyType);

        if (handler == null) {
            log.error("No handler found for strategy: {}. Available strategies: {}",
                    strategyType, handlers.keySet());
            throw new IllegalArgumentException(
                    "No inventory handler found for strategy: " + strategyType +
                            ". Available strategies: " + handlers.keySet());
        }

        log.debug("Retrieved handler for strategy: {}", strategyType);
        return handler;
    }


    public InventoryHandler getDefaultHandler() {
        InventoryStrategyType defaultStrategy = InventoryStrategyType.getDefault();
        log.debug("Retrieving default handler: {}", defaultStrategy);
        return getHandler(defaultStrategy);
    }


    public java.util.Set<InventoryStrategyType> getAvailableStrategies() {
        return handlers.keySet();
    }


    public String[] getAvailableStrategyNames() {
        return handlers.keySet().stream()
                .map(Enum::name)
                .toArray(String[]::new);
    }


    public boolean isStrategyAvailable(InventoryStrategyType strategyType) {
        return handlers.containsKey(strategyType);
    }


    private void validateAllStrategiesImplemented() {
        for (InventoryStrategyType strategyType : InventoryStrategyType.values()) {
            if (!handlers.containsKey(strategyType)) {
                log.warn("No implementation found for strategy: {} - {}",
                        strategyType, strategyType.getDescription());
            }
        }
    }
}