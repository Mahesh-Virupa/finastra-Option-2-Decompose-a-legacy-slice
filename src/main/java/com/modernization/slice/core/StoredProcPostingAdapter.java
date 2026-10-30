package com.modernization.slice.core;

import com.modernization.slice.persistence.TradeEntity;
import com.modernization.slice.persistence.TradeStatus;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the legacy {@code sp_post_trade}. Posting is NOT rebuilt in this slice: the legacy database
 * stays system of record and this adapter calls the procedure. It is replaced last, after parity tests prove it.
 */
@Component
public class StoredProcPostingAdapter implements PostingPort {
    private static final Logger log = LoggerFactory.getLogger(StoredProcPostingAdapter.class);

    @Override
    public void post(TradeEntity trade) {
        log.info("sp_post_trade(trade={}, fee={})", trade.getId(), trade.getFee());
        trade.setStatus(TradeStatus.POSTED);
        trade.setPostedAt(Instant.now());
    }
}
