package com.modernization.slice.core;

import java.util.List;

public class TradeRejectedException extends RuntimeException {
    private final List<String> reasons;

    public TradeRejectedException(List<String> reasons) {
        super("Trade rejected: " + String.join("; ", reasons));
        this.reasons = reasons;
    }

    public List<String> reasons() {
        return reasons;
    }
}
