package com.mursalin.cheque.fraud;

import com.mursalin.cheque.model.Cheque;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class FraudRules {

    private static final BigDecimal STRUCTURING_LOW  = new BigDecimal("9900.00");
    private static final BigDecimal STRUCTURING_HIGH = new BigDecimal("9999.99");

    private static final Set<String> WATCHLIST = Set.of(
        "Shell Corp Ltd",
        "Quick Cash LLC",
        "Anonymous Holdings"
    );

    public List<String> check(Cheque cheque, Set<String> seenIds) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
