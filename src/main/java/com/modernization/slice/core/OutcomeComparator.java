package com.modernization.slice.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Business-relevant differences between two outcomes (side-effect lists such as adapters are not compared). */
public final class OutcomeComparator {
    private OutcomeComparator() { }

    public static List<String> diff(TradeOutcome now, TradeOutcome legacy) {
        List<String> d = new ArrayList<>();
        if (now.accepted() != legacy.accepted()) {
            d.add("accepted: new=" + now.accepted() + " legacy=" + legacy.accepted());
            return d;
        }
        if (!now.accepted()) {
            return d;
        }
        if (!same(now.fee(), legacy.fee())) {
            d.add("fee: new=" + now.fee() + " legacy=" + legacy.fee());
        }
        if (now.approvalLevels() != legacy.approvalLevels()) {
            d.add("approvalLevels: new=" + now.approvalLevels() + " legacy=" + legacy.approvalLevels());
        }
        if (!Objects.equals(now.route(), legacy.route())) {
            d.add("route: new=" + now.route() + " legacy=" + legacy.route());
        }
        if (!Objects.equals(now.deskCode(), legacy.deskCode())) {
            d.add("deskCode: new=" + now.deskCode() + " legacy=" + legacy.deskCode());
        }
        if (!same(now.tax(), legacy.tax())) {
            d.add("tax: new=" + now.tax() + " legacy=" + legacy.tax());
        }
        return d;
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }
}
