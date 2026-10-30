package com.modernization.slice.config;

/** Strangler routing per tenant: who serves the request, and is the other side watching? */
public enum RoutingMode {
    /** New service serves. */
    NEW,
    /** New service serves; legacy logic runs alongside and every difference is counted and logged. */
    SHADOW,
    /** Legacy logic serves (instant rollback). */
    LEGACY
}
