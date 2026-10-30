package com.modernization.slice.api;

import com.modernization.slice.config.RoutingMode;
import jakarta.validation.constraints.NotNull;

public record RoutingChange(@NotNull RoutingMode mode) {
}
