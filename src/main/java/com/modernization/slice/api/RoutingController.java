package com.modernization.slice.api;

import com.modernization.slice.config.RoutingMode;
import com.modernization.slice.core.RoutingService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The strangler control plane: move a tenant NEW -> SHADOW -> LEGACY (rollback) at runtime.
 * Prototype auth: admin API key. Production: OIDC + audit trail + approval.
 */
@RestController
@RequestMapping("/admin/v1/routing")
public class RoutingController {
    private final RoutingService routing;
    private final byte[] adminKey;

    public RoutingController(RoutingService routing, @Value("${app.admin.api-key}") String adminKey) {
        this.routing = routing;
        this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping
    public Map<String, RoutingMode> all(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        auth(key);
        return routing.snapshot();
    }

    @PutMapping("/{tenantId}")
    public Map<String, RoutingMode> set(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                        @PathVariable String tenantId, @Valid @RequestBody RoutingChange change) {
        auth(key);
        routing.set(Tenants.require(tenantId), change.mode());
        return routing.snapshot();
    }

    private void auth(String presented) {
        byte[] given = presented == null ? new byte[0] : presented.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(adminKey, given)) {
            throw new UnauthorizedException("Missing or invalid X-Admin-Key");
        }
    }
}
