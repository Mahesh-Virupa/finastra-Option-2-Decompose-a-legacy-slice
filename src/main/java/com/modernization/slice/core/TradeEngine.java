package com.modernization.slice.core;

import com.modernization.slice.config.RoutingMode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The strangler facade: decides, per tenant, whether the new slice or the legacy logic serves the request,
 * and in SHADOW mode proves parity on live traffic before anyone is cut over.
 */
@Component
public class TradeEngine {
    private static final Logger log = LoggerFactory.getLogger(TradeEngine.class);

    public record Evaluation(TradeOutcome outcome, String servedBy) { }

    private final TradeProcessor newSlice;
    private final LegacyTradeProcessor legacy;
    private final RoutingService routing;
    private final Counter compared;
    private final Counter mismatched;

    public TradeEngine(TradeProcessor newSlice, LegacyTradeProcessor legacy, RoutingService routing, MeterRegistry meters) {
        this.newSlice = newSlice;
        this.legacy = legacy;
        this.routing = routing;
        this.compared = meters.counter("shadow.compared");
        this.mismatched = meters.counter("shadow.mismatch");
    }

    public Evaluation evaluate(String tenantId, TradeRequest req) {
        RoutingMode mode = routing.modeFor(tenantId);
        if (mode == RoutingMode.LEGACY) {
            return new Evaluation(legacy.evaluate(tenantId, req), "LEGACY");
        }
        TradeOutcome served = newSlice.evaluate(tenantId, req);
        if (mode == RoutingMode.SHADOW) {
            try {
                List<String> diffs = OutcomeComparator.diff(served, legacy.evaluate(tenantId, req));
                compared.increment();
                if (!diffs.isEmpty()) {
                    mismatched.increment();
                    log.warn("SHADOW MISMATCH tenant={} diffs={}", tenantId, diffs);
                }
            } catch (RuntimeException e) {
                // the shadow side must never affect the user-facing result
                log.warn("Shadow comparison failed for tenant {}: {}", tenantId, e.getMessage());
            }
        }
        return new Evaluation(served, "NEW");
    }
}
