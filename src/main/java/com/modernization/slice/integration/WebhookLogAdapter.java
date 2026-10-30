package com.modernization.slice.integration;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Modern outbound path (stub: logs the payload). A real one signs, retries with backoff and dead-letters. */
@Component
public class WebhookLogAdapter implements OutboundAdapter {
    private static final Logger log = LoggerFactory.getLogger(WebhookLogAdapter.class);
    private final MeterRegistry meters;

    public WebhookLogAdapter(MeterRegistry meters) {
        this.meters = meters;
    }

    @Override
    public String name() {
        return "webhook";
    }

    @Override
    public void deliver(TradePostedEvent e) {
        log.info("webhook <- trade.posted {}", e);
        meters.counter("adapter.delivered", "adapter", name()).increment();
    }
}
