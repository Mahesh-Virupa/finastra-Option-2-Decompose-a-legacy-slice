package com.modernization.slice;

import static org.assertj.core.api.Assertions.assertThat;

import com.modernization.slice.core.LegacyTradeProcessor;
import com.modernization.slice.core.OutcomeComparator;
import com.modernization.slice.core.TradeOutcome;
import com.modernization.slice.core.TradeProcessor;
import com.modernization.slice.core.TradeRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Characterization (golden-master) test: for every client and a spread of trades, the new slice must
 * produce exactly what the legacy logic produces. This is the safety net that lets the legacy branch be deleted.
 */
@SpringBootTest
@TestPropertySource(properties = "app.adapters.file-dir=target/test-out")
class ParityTest {

    record Case(String client, String instrument, String trader, String notional) { }

    private static final List<Case> CASES = List.of(
            new Case("CLIENT_A", "EQ", "t1", "1500000"),
            new Case("CLIENT_A", "EQ", "t2", "1000000"),     // boundary: NOT above 1M
            new Case("CLIENT_A", "FX", "t9", "500000"),      // unknown trader -> DESK-UNASSIGNED
            new Case("CLIENT_A", "EQ", "t3", "12345678"),
            new Case("CLIENT_B", "FX", "t1", "2000000"),
            new Case("CLIENT_B", "EQ", "t1", "100000"),
            new Case("CLIENT_B", "FX", "t2", "12345678"),
            new Case("CLIENT_Z", "EQ", "t1", "1500000"),     // tenant with no customization
            new Case("CLIENT_Z", "FX", "t1", "250000"));

    @Autowired TradeProcessor slice;
    @Autowired LegacyTradeProcessor legacy;

    @Test
    void newSliceMatchesLegacyForEveryCase() {
        for (Case c : CASES) {
            TradeRequest req = new TradeRequest(c.instrument(), c.trader(), new BigDecimal(c.notional()), "USD");
            TradeOutcome now = slice.evaluate(c.client(), req);
            TradeOutcome old = legacy.evaluate(c.client(), req);
            assertThat(now.accepted()).as(c.toString()).isTrue();
            assertThat(OutcomeComparator.diff(now, old)).as(c.toString()).isEmpty();
        }
    }

    @Test
    void rejectionsMatchToo() {
        TradeRequest bad = new TradeRequest("XYZ", "t1", new BigDecimal("1000"), "USD");
        assertThat(OutcomeComparator.diff(slice.evaluate("CLIENT_A", bad), legacy.evaluate("CLIENT_A", bad))).isEmpty();
        assertThat(slice.evaluate("CLIENT_A", bad).accepted()).isFalse();
    }

    @Test
    void comparatorActuallyDetectsDifferences() {
        TradeRequest req = new TradeRequest("EQ", "t1", new BigDecimal("1500000"), "USD");
        TradeOutcome a = slice.evaluate("CLIENT_A", req);
        TradeOutcome different = new TradeOutcome(true, List.of(), a.fee().add(BigDecimal.ONE), a.route(),
                a.approvalLevels(), a.deskCode(), a.tax(), a.adapters());
        assertThat(OutcomeComparator.diff(a, different)).hasSize(1).first().asString().startsWith("fee");
    }
}
