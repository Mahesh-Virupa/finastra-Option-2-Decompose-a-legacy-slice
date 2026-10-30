package com.modernization.slice.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * What the legacy code hard-coded per client, as typed configuration. Deliberately simple:
 * this is the starting point of a decomposition. Option 1 generalizes it into a governed extension platform.
 */
public record TenantPolicy(
        BigDecimal approvalAbove,
        Integer approvalLevels,
        BigDecimal feeMultiplier,
        String deskDefault,
        Map<String, String> deskTable,
        String taxRegion,
        String fxRoute,
        List<String> adapters,
        RoutingMode mode) {

    public TenantPolicy {
        if (approvalLevels == null) {
            approvalLevels = 2;
        }
        if (feeMultiplier == null) {
            feeMultiplier = BigDecimal.ONE;
        }
        if (deskTable == null) {
            deskTable = Map.of();
        }
        if (adapters == null) {
            adapters = List.of();
        }
        if (mode == null) {
            mode = RoutingMode.NEW;
        }
    }

    public boolean hasDeskLookup() {
        return deskDefault != null || !deskTable.isEmpty();
    }
}
