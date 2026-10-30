package com.modernization.slice.integration;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Bridge to the legacy FX queue (stub: logs and counts). Retired when the client moves to events/APIs. */
@Component
public class LegacyFxQueueAdapter implements OutboundAdapter {
    private static final Logger log = LoggerFactory.getLogger(LegacyFxQueueAdapter.class);
    private final MeterRegistry meters;

    public LegacyFxQueueAdapter(MeterRegistry meters) {
        this.meters = meters;
    }

    @Override
    public String name() {
        return "legacy-fx-queue";
    }

    @Override
    public void deliver(TradePostedEvent e) {
        log.info("legacy-fx-queue <- trade {} ({} {})", e.tradeId(), e.instrument(), e.notional());
        meters.counter("adapter.delivered", "adapter", name()).increment();
    }
}
