package com.modernization.slice.integration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers trade.posted to the chosen adapters AFTER COMMIT, so a rolled-back trade never leaks downstream.
 * (Production: transactional outbox + broker for at-least-once delivery.)
 */
@Component
public class AdapterDispatcher {
    private static final Logger log = LoggerFactory.getLogger(AdapterDispatcher.class);
    private final Map<String, OutboundAdapter> byName;

    public AdapterDispatcher(List<OutboundAdapter> adapters) {
        this.byName = adapters.stream().collect(Collectors.toMap(OutboundAdapter::name, Function.identity()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TradePostedEvent event) {
        Set<String> targets = new LinkedHashSet<>(event.adapters());
        if (event.route() != null && !"DEFAULT".equals(event.route())) {
            targets.add(event.route());
        }
        for (String target : targets) {
            OutboundAdapter adapter = byName.get(target);
            if (adapter == null) {
                log.warn("No adapter named '{}' for tenant {}", target, event.tenantId());
                continue;
            }
            try {
                adapter.deliver(event);
            } catch (RuntimeException ex) {
                log.error("Adapter {} failed for trade {}: {}", target, event.tradeId(), ex.getMessage());
            }
        }
    }
}
