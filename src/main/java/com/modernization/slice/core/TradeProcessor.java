package com.modernization.slice.core;

import com.modernization.slice.config.SliceProperties;
import com.modernization.slice.config.TenantPolicy;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * THE NEW SLICE. Compare with {@link LegacyTradeProcessor}: client identifiers are gone from the code;
 * what used to be {@code if (clientId.equals("CLIENT_A"))} is now a typed {@link TenantPolicy}.
 */
@Component
public class TradeProcessor {
    static final Set<String> INSTRUMENTS = Set.of("EQ", "FX", "FI", "DER", "CRYPTO");
    static final Map<String, BigDecimal> TAX_RATES = Map.of(
            "APAC", new BigDecimal("0.0005"), "EMEA", new BigDecimal("0.0003"));

    private final SliceProperties props;

    public TradeProcessor(SliceProperties props) {
        this.props = props;
    }

    /** Fail fast at startup on a misconfigured policy, not on the first trade at 2am. */
    @PostConstruct
    void validatePolicies() {
        props.tenants().forEach((tenant, policy) -> {
            if (policy.taxRegion() != null && !TAX_RATES.containsKey(policy.taxRegion())) {
                throw new IllegalStateException("Tenant " + tenant + ": unknown tax region " + policy.taxRegion());
            }
        });
    }

    public TradeOutcome evaluate(String tenantId, TradeRequest req) {
        String instrument = req.instrument().trim().toUpperCase();
        List<String> reasons = new ArrayList<>();
        if (!INSTRUMENTS.contains(instrument)) {
            reasons.add("Unsupported instrument: " + instrument);
        }
        if (req.notional() == null || req.notional().signum() <= 0) {
            reasons.add("Notional must be positive");
        }
        if (!reasons.isEmpty()) {
            return TradeOutcome.rejected(reasons);
        }

        BigDecimal notional = req.notional();
        BigDecimal fee = notional.multiply(props.feeBps()).divide(new BigDecimal("10000"), 8, RoundingMode.HALF_UP);
        int approvals = 0;
        String desk = null;
        BigDecimal tax = null;
        String route = "DEFAULT";
        List<String> adapters = List.of();

        TenantPolicy p = props.policy(tenantId).orElse(null);
        if (p != null) {
            if (p.approvalAbove() != null && notional.compareTo(p.approvalAbove()) > 0) {
                approvals = p.approvalLevels();
            }
            if (p.hasDeskLookup()) {
                desk = p.deskTable().getOrDefault(req.trader(), p.deskDefault());
            }
            fee = fee.multiply(p.feeMultiplier());
            if (p.taxRegion() != null) {
                tax = notional.multiply(TAX_RATES.get(p.taxRegion())).setScale(2, RoundingMode.HALF_UP);
            }
            if (p.fxRoute() != null && "FX".equals(instrument)) {
                route = p.fxRoute();
            }
            adapters = p.adapters();
        }
        return new TradeOutcome(true, List.of(), fee.setScale(2, RoundingMode.HALF_UP), route, approvals, desk, tax, adapters);
    }
}
