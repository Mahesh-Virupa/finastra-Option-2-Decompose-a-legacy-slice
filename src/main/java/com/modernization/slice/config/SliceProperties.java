package com.modernization.slice.config;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slice")
public record SliceProperties(BigDecimal feeBps, Map<String, TenantPolicy> tenants) {
    public SliceProperties {
        if (feeBps == null) {
            feeBps = new BigDecimal("2");
        }
        if (tenants == null) {
            tenants = Map.of();
        }
    }

    public Optional<TenantPolicy> policy(String tenantId) {
        return Optional.ofNullable(tenants.get(tenantId));
    }
}
