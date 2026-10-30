package com.modernization.slice.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Puts the tenant on every log line. */
@Component
public class TenantMdcFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenant = request.getHeader("X-Tenant-Id");
        if (tenant != null && tenant.length() <= 64) {
            MDC.put("tenant", tenant.replaceAll("[^A-Za-z0-9_-]", "_"));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("tenant");
        }
    }
}
