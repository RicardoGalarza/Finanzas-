package cl.flujoclaro.adapters.in.rest.dto;

import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovementRequests {
    public static class IncomeRequest {
        @NotBlank
        public String description;
        @NotNull
        @DecimalMin("0.01")
        public BigDecimal amount;
        @NotNull
        public LocalDate incomeDate;
        @NotBlank
        public String category;
        @NotBlank
        public String receivedBy;
        @NotNull
        public RecurrenceType incomeType;
        public Frequency frequency;
        public String paymentMethod;
        public String notes;
    }

    public static class ExpenseRequest {
        @NotBlank
        public String name;
        @NotNull
        @DecimalMin("0.01")
        public BigDecimal amount;
        @NotNull
        public LocalDate dueDate;
        @NotBlank
        public String category;
        @NotBlank
        public String responsiblePerson;
        @NotNull
        public RecurrenceType expenseType;
        public Frequency frequency;
        public LocalDate recurrenceEndDate;
        public String paymentMethod;
        public String notes;
    }

    public static class PayExpenseRequest {
        public LocalDate paidAt;
    }
}
