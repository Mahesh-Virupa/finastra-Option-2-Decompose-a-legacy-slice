package com.modernization.slice.core;

import com.modernization.slice.config.RoutingMode;
import com.modernization.slice.config.SliceProperties;
import com.modernization.slice.config.TenantPolicy;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-tenant strangler switch. Defaults come from configuration; runtime overrides give instant rollback.
 * (Overrides are in-memory per replica: production keeps this in a config service / database with change events.)
 */
@Component
public class RoutingService {
    private final SliceProperties props;
    private final Map<String, RoutingMode> overrides = new ConcurrentHashMap<>();

    public RoutingService(SliceProperties props) {
        this.props = props;
    }

    public RoutingMode modeFor(String tenantId) {
        RoutingMode override = overrides.get(tenantId);
        if (override != null) {
            return override;
        }
        return props.policy(tenantId).map(TenantPolicy::mode).orElse(RoutingMode.NEW);
    }

    public void set(String tenantId, RoutingMode mode) {
        overrides.put(tenantId, mode);
    }

    public Map<String, RoutingMode> snapshot() {
        Map<String, RoutingMode> out = new TreeMap<>();
        props.tenants().forEach((t, p) -> out.put(t, modeFor(t)));
        overrides.forEach((t, m) -> out.put(t, m));
        return out;
    }
}
