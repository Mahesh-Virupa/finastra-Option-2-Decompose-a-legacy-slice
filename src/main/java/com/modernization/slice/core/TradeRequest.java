package com.modernization.slice.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Public contract for submitting a trade (see static/openapi.yaml). */
public record TradeRequest(
        @NotBlank String instrument,
        @NotBlank String trader,
        @NotNull @Positive BigDecimal notional,
        String currency) {

    public TradeRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
