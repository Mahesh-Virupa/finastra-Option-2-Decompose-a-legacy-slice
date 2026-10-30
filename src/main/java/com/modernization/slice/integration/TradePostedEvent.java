package com.modernization.slice.integration;

import java.math.BigDecimal;
import java.util.List;

/** Domain event emitted once a trade is POSTED. Delivered after commit. */
public record TradePostedEvent(
        String tradeId,
        String tenantId,
        String instrument,
        String trader,
        BigDecimal notional,
        BigDecimal fee,
        String route,
        String deskCode,
        BigDecimal tax,
        List<String> adapters) {
}
