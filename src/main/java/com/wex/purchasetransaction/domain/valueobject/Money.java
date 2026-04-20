package com.wex.purchasetransaction.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final int SCALE = 2;

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        amount = amount.setScale(SCALE, ROUNDING_MODE);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value");
        }
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money multiply(BigDecimal factor) {
        if (factor == null) {
            throw new IllegalArgumentException("Factor must not be null");
        }
        return new Money(amount.multiply(factor).setScale(SCALE, ROUNDING_MODE));
    }

    public BigDecimal value() {
        return amount;
    }
}
