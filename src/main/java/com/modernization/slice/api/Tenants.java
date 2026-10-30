package com.modernization.slice.api;

import java.util.regex.Pattern;

final class Tenants {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private Tenants() { }

    /** Production: tenant comes from the gateway/JWT. Here: a validated header. */
    static String require(String tenantId) {
        if (tenantId == null || !VALID.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("X-Tenant-Id must match [A-Za-z0-9_-]{1,64}");
        }
        return tenantId;
    }
}
