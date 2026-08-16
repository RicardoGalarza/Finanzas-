package cl.flujoclaro.domain;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.model.FinancialSummaryCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void recurringExpenseCalculatesNextDueDate() {
        Expense expense = Expense.create(
                UUID.randomUUID(),
                "Cuota auto",
                new BigDecimal("475000"),
                LocalDate.of(2026, 8, 31),
                "Auto / cuota",
                "Ricardo",
                RecurrenceType.RECURRING,
                Frequency.MONTHLY,
                "Banco Estado",
                null,
                UUID.randomUUID()
        );

        assertEquals(LocalDate.of(2026, 9, 30), expense.nextDueDate());
    }

    @Test
    void recurringExpenseStopsAfterLastInstallment() {
        Expense expense = Expense.create(
                UUID.randomUUID(),
                "Cuota auto",
                new BigDecimal("475000"),
                LocalDate.of(2026, 12, 31),
                "Auto / cuota",
                "Ricardo",
                RecurrenceType.RECURRING,
                Frequency.MONTHLY,
                LocalDate.of(2026, 12, 31),
                "Banco Estado",
                null,
                UUID.randomUUID()
        );

        assertNull(expense.nextDueDate());
    }
}
