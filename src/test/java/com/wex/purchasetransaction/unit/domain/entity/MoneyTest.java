package com.wex.purchasetransaction.unit.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wex.purchasetransaction.domain.valueobject.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void of_shouldRoundToTwoDecimalPlaces() {
        Money money = Money.of(new BigDecimal("10.555"));
        assertThat(money.value()).isEqualByComparingTo("10.56");
    }

    @Test
    void of_shouldAcceptMinimumValidAmount() {
        Money money = Money.of(new BigDecimal("0.01"));
        assertThat(money.value()).isEqualByComparingTo("0.01");
    }

    @Test
    void of_shouldFailWhenAmountIsNull() {
        assertThatThrownBy(() -> Money.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void of_shouldFailWhenAmountIsZero() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void of_shouldFailWhenAmountIsNegative() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void of_shouldFailWhenAmountBelowMinimumAfterRounding() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("0.001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void multiply_shouldReturnCorrectConvertedAmount() {
        Money money = Money.of(new BigDecimal("100.00"));
        Money result = money.multiply(new BigDecimal("4.97"));
        assertThat(result.value()).isEqualByComparingTo("497.00");
    }

    @Test
    void multiply_shouldRoundConvertedAmountHalfUp() {
        Money money = Money.of(new BigDecimal("10.00"));
        Money result = money.multiply(new BigDecimal("1.005"));
        assertThat(result.value()).isEqualByComparingTo("10.05");
    }

    @Test
    void multiply_shouldHandleHighInflationRate() {
        Money money = Money.of(new BigDecimal("1.00"));
        Money result = money.multiply(new BigDecimal("1500.50"));
        assertThat(result.value()).isEqualByComparingTo("1500.50");
    }

    @Test
    void multiply_shouldHandleLargeAmount() {
        Money money = Money.of(new BigDecimal("99999999.99"));
        Money result = money.multiply(new BigDecimal("5.00"));
        assertThat(result.value()).isEqualByComparingTo("499999999.95");
    }

    @Test
    void multiply_shouldFailWhenFactorIsNull() {
        Money money = Money.of(new BigDecimal("100.00"));
        assertThatThrownBy(() -> money.multiply(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void multiply_shouldRoundCorrectlyAtHalfwayPoint() {
        // 10.00 * 1.005 = 10.05 (HALF_UP: .005 rounds up)
        Money money = Money.of(new BigDecimal("10.00"));
        Money result = money.multiply(new BigDecimal("1.005"));
        assertThat(result.value()).isEqualByComparingTo("10.05");
    }

    @Test
    void multiply_shouldRoundDownWhenBelowHalfway() {
        // 10.00 * 1.004 = 10.04
        Money money = Money.of(new BigDecimal("10.00"));
        Money result = money.multiply(new BigDecimal("1.004"));
        assertThat(result.value()).isEqualByComparingTo("10.04");
    }

    @Test
    void multiply_shouldHandleVerySmallAmountWithHighRate() {
        // $0.01 * 1500 = $15.00
        Money money = Money.of(new BigDecimal("0.01"));
        Money result = money.multiply(new BigDecimal("1500.00"));
        assertThat(result.value()).isEqualByComparingTo("15.00");
    }

    @Test
    void multiply_shouldHandleExchangeRateWithManyDecimals() {
        // 100.00 * 5.123456789 = 512.35 (rounded)
        Money money = Money.of(new BigDecimal("100.00"));
        Money result = money.multiply(new BigDecimal("5.123456789"));
        assertThat(result.value()).isEqualByComparingTo("512.35");
    }

    @Test
    void multiply_shouldHandleVerySmallExchangeRate() {
        // 1000.00 * 0.0001 = 0.10
        Money money = Money.of(new BigDecimal("1000.00"));
        Money result = money.multiply(new BigDecimal("0.0001"));
        assertThat(result.value()).isEqualByComparingTo("0.10");
    }

    @Test
    void multiply_shouldHandleExchangeRateOfOne() {
        Money money = Money.of(new BigDecimal("123.45"));
        Money result = money.multiply(BigDecimal.ONE);
        assertThat(result.value()).isEqualByComparingTo("123.45");
    }

    @Test
    void of_shouldRoundHalfUpCorrectly() {
        // 10.555 should round to 10.56 (HALF_UP)
        Money money = Money.of(new BigDecimal("10.555"));
        assertThat(money.value()).isEqualByComparingTo("10.56");
    }

    @Test
    void of_shouldRoundDownWhenBelowHalfway() {
        // 10.554 should round to 10.55
        Money money = Money.of(new BigDecimal("10.554"));
        assertThat(money.value()).isEqualByComparingTo("10.55");
    }

    @Test
    void of_shouldHandleVeryLargePrecision() {
        // Test with many decimal places
        Money money = Money.of(new BigDecimal("10.123456789012345"));
        assertThat(money.value()).isEqualByComparingTo("10.12");
    }

    @Test
    void multiply_shouldProduceCorrectResultForTypicalBrazilConversion() {
        // Typical BRL conversion: $200 * 5.00 = R$1000.00
        Money money = Money.of(new BigDecimal("200.00"));
        Money result = money.multiply(new BigDecimal("5.00"));
        assertThat(result.value()).isEqualByComparingTo("1000.00");
    }

    @Test
    void multiply_shouldProduceCorrectResultForTypicalYenConversion() {
        // Typical JPY conversion: $100 * 149.75 = ¥14,975.00
        Money money = Money.of(new BigDecimal("100.00"));
        Money result = money.multiply(new BigDecimal("149.75"));
        assertThat(result.value()).isEqualByComparingTo("14975.00");
    }

    @Test
    void multiply_shouldProduceCorrectResultForTypicalEuroConversion() {
        // Typical EUR conversion: $100 * 0.92 = €92.00
        Money money = Money.of(new BigDecimal("100.00"));
        Money result = money.multiply(new BigDecimal("0.92"));
        assertThat(result.value()).isEqualByComparingTo("92.00");
    }
}
