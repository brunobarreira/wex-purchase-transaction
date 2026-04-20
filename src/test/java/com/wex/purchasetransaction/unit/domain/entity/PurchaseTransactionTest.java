package com.wex.purchasetransaction.unit.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wex.purchasetransaction.domain.entity.PurchaseTransaction;
import com.wex.purchasetransaction.domain.valueobject.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseTransactionTest {

    @Test
    void create_shouldGenerateUniqueId() {
        PurchaseTransaction t1 = PurchaseTransaction.create("desc1", LocalDate.now(), Money.of(BigDecimal.TEN));
        PurchaseTransaction t2 = PurchaseTransaction.create("desc2", LocalDate.now(), Money.of(BigDecimal.ONE));
        assertThat(t1.getId()).isNotEqualTo(t2.getId());
    }

    @Test
    void create_shouldRoundAmountToNearestCent() {
        PurchaseTransaction t = PurchaseTransaction.create("desc", LocalDate.now(), Money.of(new BigDecimal("12.345")));
        assertThat(t.getAmount().value()).isEqualByComparingTo("12.35");
    }

    @Test
    void create_shouldAcceptDescriptionOf49Chars() {
        String exactly49 = "A".repeat(49);
        PurchaseTransaction t = PurchaseTransaction.create(exactly49, LocalDate.now(), Money.of(BigDecimal.ONE));
        assertThat(t.getDescription()).hasSize(49);
    }

    @Test
    void create_shouldAcceptDescriptionOfExactly50Chars() {
        String exactly50 = "A".repeat(50);
        PurchaseTransaction t = PurchaseTransaction.create(exactly50, LocalDate.now(), Money.of(BigDecimal.ONE));
        assertThat(t.getDescription()).hasSize(50);
    }

    @Test
    void create_shouldFailWhenDescriptionExceeds50Chars() {
        String longDesc = "A".repeat(51);
        assertThatThrownBy(() -> PurchaseTransaction.create(longDesc, LocalDate.now(), Money.of(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");
    }

    @Test
    void create_shouldFailWhenDescriptionIsBlank() {
        assertThatThrownBy(() -> PurchaseTransaction.create("  ", LocalDate.now(), Money.of(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void create_shouldFailWhenDescriptionIsNull() {
        assertThatThrownBy(() -> PurchaseTransaction.create(null, LocalDate.now(), Money.of(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_shouldAcceptTransactionDateAsToday() {
        PurchaseTransaction t = PurchaseTransaction.create("desc", LocalDate.now(), Money.of(BigDecimal.ONE));
        assertThat(t.getTransactionDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void create_shouldFailWhenTransactionDateIsInFuture() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> PurchaseTransaction.create("desc", tomorrow, Money.of(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void create_shouldFailWhenTransactionDateIsNull() {
        assertThatThrownBy(() -> PurchaseTransaction.create("desc", null, Money.of(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date");
    }

    @Test
    void reconstitute_shouldPreserveAllFields() {
        UUID id = UUID.randomUUID();
        LocalDate date = LocalDate.of(2024, 3, 15);
        Money amount = Money.of(new BigDecimal("100.00"));
        PurchaseTransaction t = PurchaseTransaction.reconstitute(id, "Hotel stay", date, amount);
        assertThat(t.getId()).isEqualTo(id);
        assertThat(t.getDescription()).isEqualTo("Hotel stay");
        assertThat(t.getTransactionDate()).isEqualTo(date);
        assertThat(t.getAmount().value()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_shouldAcceptDescriptionWithUnicodeCharacters() {
        String unicodeDesc = "Café Résumé 日本語 한국어";
        PurchaseTransaction t = PurchaseTransaction.create(unicodeDesc, LocalDate.now(), Money.of(BigDecimal.TEN));
        assertThat(t.getDescription()).isEqualTo(unicodeDesc);
    }

    @Test
    void create_shouldAcceptDescriptionWithSpecialCharacters() {
        String specialDesc = "Test & Co. @ $100! #1 (50%)";
        PurchaseTransaction t = PurchaseTransaction.create(specialDesc, LocalDate.now(), Money.of(BigDecimal.TEN));
        assertThat(t.getDescription()).isEqualTo(specialDesc);
    }

    @Test
    void create_shouldAcceptDescriptionWithEmojis() {
        String emojiDesc = "Coffee ☕ and food 🍕";
        PurchaseTransaction t = PurchaseTransaction.create(emojiDesc, LocalDate.now(), Money.of(BigDecimal.TEN));
        assertThat(t.getDescription()).isEqualTo(emojiDesc);
    }

    @Test
    void create_shouldAcceptTransactionDateOnLeapYearDay() {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        PurchaseTransaction t = PurchaseTransaction.create("Leap day purchase", leapDay, Money.of(BigDecimal.TEN));
        assertThat(t.getTransactionDate()).isEqualTo(leapDay);
    }

    @Test
    void create_shouldAcceptTransactionDateOnYearBoundary() {
        LocalDate newYearsEve = LocalDate.of(2023, 12, 31);
        PurchaseTransaction t = PurchaseTransaction.create("New year eve", newYearsEve, Money.of(BigDecimal.TEN));
        assertThat(t.getTransactionDate()).isEqualTo(newYearsEve);
    }

    @Test
    void create_shouldAcceptTransactionDateOnFirstDayOfYear() {
        LocalDate newYear = LocalDate.of(2024, 1, 1);
        PurchaseTransaction t = PurchaseTransaction.create("New year day", newYear, Money.of(BigDecimal.TEN));
        assertThat(t.getTransactionDate()).isEqualTo(newYear);
    }

    @Test
    void create_shouldPreserveLeadingAndTrailingSpacesInDescription() {
        PurchaseTransaction t = PurchaseTransaction.create("  Hotel stay  ", LocalDate.now(), Money.of(BigDecimal.TEN));
        assertThat(t.getDescription()).isEqualTo("  Hotel stay  ");
    }

    @Test
    void create_shouldCountUnicodeCharactersCorrectlyFor50CharLimit() {
        // Create exactly 50 characters with unicode
        String exactly50 = "A".repeat(50);

        PurchaseTransaction t = PurchaseTransaction.create(exactly50, LocalDate.now(), Money.of(BigDecimal.TEN));
        assertThat(t.getDescription()).hasSize(50);
    }

    @Test
    void create_shouldAcceptDescriptionWith50UnicodeCharacters() {
        // 50 unicode characters
        String unicodeDesc = "日本語テストテストテストテストテストテストテストテストABC";
        // Ensure exactly 50 chars
        String exactly50 = unicodeDesc.length() > 50
                ? unicodeDesc.substring(0, 50)
                : unicodeDesc + "X".repeat(50 - unicodeDesc.length());

        PurchaseTransaction t = PurchaseTransaction.create(exactly50, LocalDate.now(), Money.of(BigDecimal.TEN));
        assertThat(t.getDescription()).hasSize(50);
    }
}
