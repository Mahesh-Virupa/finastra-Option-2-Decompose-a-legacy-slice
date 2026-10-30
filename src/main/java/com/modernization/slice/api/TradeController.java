package com.modernization.slice.api;

import com.modernization.slice.core.TradeOutcome;
import com.modernization.slice.core.TradeRequest;
import com.modernization.slice.core.TradeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The extracted slice's public contract. See static/openapi.yaml (served at /openapi.yaml). */
@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final TradeService trades;

    public TradeController(TradeService trades) {
        this.trades = trades;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> create(@RequestHeader("X-Tenant-Id") String tenant,
                                                @Valid @RequestBody TradeRequest request) {
        TradeResponse created = trades.create(Tenants.require(tenant), request);
        return ResponseEntity.created(URI.create("/api/v1/trades/" + created.id())).body(created);
    }

    @PostMapping("/simulate")
    public TradeOutcome simulate(@RequestHeader("X-Tenant-Id") String tenant, @Valid @RequestBody TradeRequest request) {
        return trades.simulate(Tenants.require(tenant), request);
    }

    @GetMapping("/{id}")
    public TradeResponse get(@RequestHeader("X-Tenant-Id") String tenant, @PathVariable String id) {
        return trades.get(Tenants.require(tenant), id);
    }

    @GetMapping
    public List<TradeResponse> list(@RequestHeader("X-Tenant-Id") String tenant) {
        return trades.list(Tenants.require(tenant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-Tenant-Id") String tenant, @PathVariable String id) {
        trades.delete(Tenants.require(tenant), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approvals")
    public TradeResponse approve(@RequestHeader("X-Tenant-Id") String tenant, @PathVariable String id) {
        return trades.approve(Tenants.require(tenant), id);
    }
}
