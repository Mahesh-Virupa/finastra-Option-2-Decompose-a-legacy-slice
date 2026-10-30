package com.modernization.slice.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * STAND-IN FOR THE LEGACY SYSTEM (Appendix A): client ids hard-coded in the pipeline, double arithmetic.
 * In a real migration this is a call through an anti-corruption layer to the existing platform.
 * It exists in this repo so the strangler routing, shadow comparison and instant rollback are demonstrable.
 * DELETE this class (and the SHADOW/LEGACY modes) once every tenant is on the new slice.
 */
@Component
public class LegacyTradeProcessor {
    private static final Map<String, String> DESKS = Map.of("t1", "DESK-NY", "t2", "DESK-LDN", "t3", "DESK-SGP");

    public TradeOutcome evaluate(String clientId, TradeRequest req) {
        String instrument = req.instrument().trim().toUpperCase();
        List<String> reasons = new ArrayList<>();
        if (!TradeProcessor.INSTRUMENTS.contains(instrument)) {
            reasons.add("Unsupported instrument: " + instrument);
        }
        if (req.notional() == null || req.notional().signum() <= 0) {
            reasons.add("Notional must be positive");
        }
        if (!reasons.isEmpty()) {
            return TradeOutcome.rejected(reasons);
        }

        double fee = req.notional().doubleValue() * 0.0002;
        String deskCode = null;
        int approvals = 0;
        String route = "DEFAULT";
        BigDecimal tax = null;

        if (clientId.equals("CLIENT_A")) {
            if (req.notional().doubleValue() > 1_000_000) {
                approvals = 2;                                    // requireDualApproval(trade)
            }
            deskCode = DESKS.getOrDefault(req.trader(), "DESK-UNASSIGNED");  // lookupDeskCode(trade.trader)
            fee = fee * 0.9;                                      // 10% discount
        } else if (clientId.equals("CLIENT_B")) {
            tax = req.notional().multiply(new BigDecimal("0.0005")).setScale(2, RoundingMode.HALF_UP); // applyRegionalTaxRules('APAC')
            if (instrument.equals("FX")) {
                route = "legacy-fx-queue";                        // routeToLegacyFxQueue(trade)
            }
        }
        // the legacy system wrote a trade file for every client
        return new TradeOutcome(true, List.of(), BigDecimal.valueOf(fee).setScale(2, RoundingMode.HALF_UP),
                route, approvals, deskCode, tax, List.of("file-feed"));
    }
}
