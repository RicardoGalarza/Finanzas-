package cl.flujoclaro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public final class FinancialSummaryCalculator {

    private FinancialSummaryCalculator() {
    }

    public static BigDecimal currentBalance(BigDecimal initialBalance, BigDecimal totalIncomes, BigDecimal totalPaidExpenses) {
        return safe(initialBalance).add(safe(totalIncomes)).subtract(safe(totalPaidExpenses));
    }

    public static BigDecimal availableMoney(BigDecimal currentBalance, BigDecimal pendingObligations) {
        return safe(currentBalance).subtract(safe(pendingObligations));
    }

    public static BigDecimal incomeUsagePercentage(BigDecimal monthlyIncomes, BigDecimal monthlyPaidExpenses) {
        if (safe(monthlyIncomes).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safe(monthlyPaidExpenses)
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyIncomes, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().map(FinancialSummaryCalculator::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal categoryTotal(Map<String, BigDecimal> byCategory, String category) {
        return safe(byCategory.get(category));
    }
}
