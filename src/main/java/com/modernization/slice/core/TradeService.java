package com.modernization.slice.core;

import com.modernization.slice.api.TradeResponse;
import com.modernization.slice.integration.TradePostedEvent;
import com.modernization.slice.persistence.TradeEntity;
import com.modernization.slice.persistence.TradeRepository;
import com.modernization.slice.persistence.TradeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeService {
    private final TradeRepository repo;
    private final TradeEngine engine;
    private final PostingPort posting;
    private final ApplicationEventPublisher events;

    public TradeService(TradeRepository repo, TradeEngine engine, PostingPort posting, ApplicationEventPublisher events) {
        this.repo = repo;
        this.engine = engine;
        this.posting = posting;
        this.events = events;
    }

    @Transactional
    public TradeResponse create(String tenantId, TradeRequest req) {
        TradeEngine.Evaluation ev = engine.evaluate(tenantId, req);
        TradeOutcome o = ev.outcome();
        if (!o.accepted()) {
            throw new TradeRejectedException(o.reasons());
        }
        TradeEntity e = new TradeEntity();
        e.setId(UUID.randomUUID().toString());
        e.setTenantId(tenantId);
        e.setInstrument(req.instrument().trim().toUpperCase());
        e.setTrader(req.trader());
        e.setNotional(req.notional());
        e.setCurrency(req.currency());
        e.setFee(o.fee());
        e.setRoute(o.route());
        e.setDeskCode(o.deskCode());
        e.setTax(o.tax());
        e.setServedBy(ev.servedBy());
        e.setAdaptersCsv(String.join(",", o.adapters()));
        e.setCreatedAt(Instant.now());
        e.setApprovalsRemaining(o.approvalLevels());
        e.setStatus(o.approvalLevels() > 0 ? TradeStatus.PENDING_APPROVAL : TradeStatus.POSTED);
        if (e.getStatus() == TradeStatus.POSTED) {
            postAndPublish(e);
        }
        repo.save(e);
        return TradeResponse.from(e);
    }

    /** Dry run through the same routing as a real trade; nothing is persisted or published. */
    public TradeOutcome simulate(String tenantId, TradeRequest req) {
        return engine.evaluate(tenantId, req).outcome();
    }

    @Transactional
    public TradeResponse approve(String tenantId, String id) {
        TradeEntity e = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Trade not found: " + id));
        if (e.getStatus() != TradeStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Trade " + id + " is not awaiting approval");
        }
        e.setApprovalsRemaining(e.getApprovalsRemaining() - 1);
        if (e.getApprovalsRemaining() == 0) {
            postAndPublish(e);
        }
        repo.save(e);
        return TradeResponse.from(e);
    }

    @Transactional(readOnly = true)
    public TradeResponse get(String tenantId, String id) {
        return repo.findByIdAndTenantId(id, tenantId).map(TradeResponse::from)
                .orElseThrow(() -> new NotFoundException("Trade not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> list(String tenantId) {
        return repo.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(TradeResponse::from).toList();
    }

    @Transactional
    public void delete(String tenantId, String id) {
        TradeEntity e = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Trade not found: " + id));
        repo.delete(e);
    }

    private void postAndPublish(TradeEntity e) {
        posting.post(e);
        List<String> adapters = e.getAdaptersCsv() == null || e.getAdaptersCsv().isBlank()
                ? List.of() : List.of(e.getAdaptersCsv().split(","));
        events.publishEvent(new TradePostedEvent(e.getId(), e.getTenantId(), e.getInstrument(), e.getTrader(),
                e.getNotional(), e.getFee(), e.getRoute(), e.getDeskCode(), e.getTax(), adapters));
    }
}
