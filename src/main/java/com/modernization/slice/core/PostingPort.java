package com.modernization.slice.core;

import com.modernization.slice.persistence.TradeEntity;

/** Anti-corruption layer around legacy posting: callers never know a stored procedure is behind it. */
public interface PostingPort {
    void post(TradeEntity trade);
}
