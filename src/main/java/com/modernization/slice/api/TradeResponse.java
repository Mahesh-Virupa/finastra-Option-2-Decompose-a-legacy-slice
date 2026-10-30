package com.modernization.slice.api;

import com.modernization.slice.persistence.TradeEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TradeResponse(
        String id,
        String tenantId,
        String status,
        String instrument,
        String trader,
        BigDecimal notional,
        String currency,
        BigDecimal fee,
        String route,
        int approvalsRemaining,
        String deskCode,
        BigDecimal tax,
        String servedBy,
        List<String> adapters,
        Instant createdAt) {

    public static TradeResponse from(TradeEntity e) {
        List<String> adapters = e.getAdaptersCsv() == null || e.getAdaptersCsv().isBlank()
                ? List.of() : List.of(e.getAdaptersCsv().split(","));
        return new TradeResponse(e.getId(), e.getTenantId(), e.getStatus().name(), e.getInstrument(), e.getTrader(),
                e.getNotional(), e.getCurrency(), e.getFee(), e.getRoute(), e.getApprovalsRemaining(),
                e.getDeskCode(), e.getTax(), e.getServedBy(), adapters, e.getCreatedAt());
    }
}
