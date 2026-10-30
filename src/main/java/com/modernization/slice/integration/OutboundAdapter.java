package com.modernization.slice.integration;

/** Reproduces a legacy interface (file drop, queue) or offers a modern one (webhook). */
public interface OutboundAdapter {
    String name();

    void deliver(TradePostedEvent event);
}
