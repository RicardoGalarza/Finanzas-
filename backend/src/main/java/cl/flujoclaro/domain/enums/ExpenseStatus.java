package cl.flujoclaro.domain.enums;

import java.time.LocalDate;

public enum ExpenseStatus {
    PENDING,
    PAID,
    OVERDUE;

    public static ExpenseStatus effective(ExpenseStatus stored, LocalDate dueDate, LocalDate today) {
        if (stored == PAID) {
            return PAID;
        }
        if (dueDate.isBefore(today)) {
            return OVERDUE;
        }
        return PENDING;
    }
}
