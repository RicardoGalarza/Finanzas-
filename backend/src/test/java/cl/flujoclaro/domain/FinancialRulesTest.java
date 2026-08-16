package cl.flujoclaro.domain;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.model.FinancialSummaryCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialRulesTest {

    @Test
    void availableMoneySubtractsPendingObligations() {
        BigDecimal available = FinancialSummaryCalculator.availableMoney(
                new BigDecimal("1000000"),
                new BigDecimal("250000")
        );
        assertEquals(new BigDecimal("750000"), available);
    }

    @Test
    void currentBalanceUsesInitialPlusIncomesMinusPaid() {
        BigDecimal balance = FinancialSummaryCalculator.currentBalance(
                new BigDecimal("100000"),
                new BigDecimal("500000"),
                new BigDecimal("200000")
        );
        assertEquals(new BigDecimal("400000"), balance);
    }

    @Test
    void overdueWhenDueDatePassedAndNotPaid() {
        assertEquals(ExpenseStatus.OVERDUE,
                ExpenseStatus.effective(ExpenseStatus.PENDING, LocalDate.now().minusDays(1), LocalDate.now()));
        assertEquals(ExpenseStatus.PENDING,
                ExpenseStatus.effective(ExpenseStatus.PENDING, LocalDate.now().plusDays(1), LocalDate.now()));
        assertEquals(ExpenseStatus.PAID,
                ExpenseStatus.effective(ExpenseStatus.PAID, LocalDate.now().minusDays(5), LocalDate.now()));
    }
}
