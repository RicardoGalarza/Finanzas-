package cl.flujoclaro.adapters.in.rest.dto;

import cl.flujoclaro.domain.enums.Frequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class OnboardingRequest {
    @NotBlank
    public String fullName;
    @NotBlank
    public String country;
    @NotBlank
    public String currencyCode;
    @NotNull
    @DecimalMin("0")
    public BigDecimal initialBalance;
    public boolean shared;
    public String spaceName;
    @Valid
    public List<HabitualIncomeRequest> incomes;
    @Valid
    public List<HabitualBillRequest> bills;

    public static class HabitualIncomeRequest {
        @NotBlank
        public String description;
        @NotNull
        @DecimalMin("0.01")
        public BigDecimal amount;
        public String category;
        public Frequency frequency;
    }

    public static class HabitualBillRequest {
        @NotBlank
        public String name;
        @NotNull
        @DecimalMin("0.01")
        public BigDecimal amount;
        public String category;
        public int dueDay = 1;
    }
}
