package com.modernization.slice.core;

import java.math.BigDecimal;
import java.util.List;

/** Pure result of the pipeline: no persistence, no side effects. Both implementations (new and legacy) return this. */
public record TradeOutcome(
        boolean accepted,
        List<String> reasons,
        BigDecimal fee,
        String route,
        int approvalLevels,
        String deskCode,
        BigDecimal tax,
        List<String> adapters) {

    public static TradeOutcome rejected(List<String> reasons) {
        return new TradeOutcome(false, List.copyOf(reasons), null, null, 0, null, null, List.of());
    }
}
