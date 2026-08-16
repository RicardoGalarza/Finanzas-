package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.enums.SpaceType;
import cl.flujoclaro.domain.exception.DomainException;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.model.FinancialSpace;
import cl.flujoclaro.domain.model.Income;
import cl.flujoclaro.domain.model.Membership;
import cl.flujoclaro.domain.model.User;
import cl.flujoclaro.domain.port.ExpenseRepositoryPort;
import cl.flujoclaro.domain.port.IncomeRepositoryPort;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import cl.flujoclaro.domain.port.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OnboardingService {

    public record HabitualIncome(String description, BigDecimal amount, String category, String frequency) {}
    public record HabitualBill(String name, BigDecimal amount, String category, int dueDay) {}
    public record OnboardingCommand(
            String fullName,
            String country,
            String currencyCode,
            BigDecimal initialBalance,
            boolean shared,
            String spaceName,
            List<HabitualIncome> incomes,
            List<HabitualBill> bills
    ) {}

    private final UserRepositoryPort userRepository;
    private final SpaceRepositoryPort spaceRepository;
    private final IncomeRepositoryPort incomeRepository;
    private final ExpenseRepositoryPort expenseRepository;

    public OnboardingService(UserRepositoryPort userRepository,
                             SpaceRepositoryPort spaceRepository,
                             IncomeRepositoryPort incomeRepository,
                             ExpenseRepositoryPort expenseRepository) {
        this.userRepository = userRepository;
        this.spaceRepository = spaceRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public FinancialSpace complete(UUID userId, OnboardingCommand command) {
        if (command.fullName() == null || command.fullName().isBlank()) {
            throw new DomainException("El nombre es obligatorio");
        }
        if (command.currencyCode() == null || command.currencyCode().isBlank()) {
            throw new DomainException("La moneda es obligatoria");
        }
        if (command.initialBalance() != null && command.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("El saldo inicial no puede ser negativo");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Usuario no encontrado"));
        user.completeOnboarding(command.fullName(), command.country(), command.currencyCode());
        userRepository.save(user);

        Membership membership = spaceRepository.findMembershipsByUser(userId).stream()
                .findFirst()
                .orElseThrow(() -> new DomainException("No se encontró espacio financiero"));
        FinancialSpace space = spaceRepository.findById(membership.getSpaceId())
                .orElseThrow(() -> new DomainException("Espacio no encontrado"));

        space.setName(command.spaceName() != null && !command.spaceName().isBlank()
                ? command.spaceName()
                : (command.shared() ? "Espacio familiar" : "Espacio personal"));
        space.setType(command.shared() ? SpaceType.SHARED : SpaceType.PERSONAL);
        space.setCurrencyCode(command.currencyCode());
        space.setInitialBalance(command.initialBalance() != null ? command.initialBalance() : BigDecimal.ZERO);
        space.setUpdatedAt(java.time.Instant.now());
        spaceRepository.save(space);

        LocalDate today = LocalDate.now();
        if (command.incomes() != null) {
            for (HabitualIncome item : command.incomes()) {
                if (item.amount() == null || item.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                Income income = Income.create(
                        space.getId(),
                        item.description(),
                        item.amount(),
                        today,
                        item.category() != null ? item.category() : "Sueldo",
                        user.getFullName(),
                        RecurrenceType.RECURRING,
                        parseFrequency(item.frequency()),
                        "Cuenta bancaria",
                        "Creado en onboarding",
                        userId
                );
                incomeRepository.save(income);
            }
        }

        if (command.bills() != null) {
            for (HabitualBill bill : command.bills()) {
                if (bill.amount() == null || bill.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                int day = Math.min(Math.max(bill.dueDay(), 1), 28);
                LocalDate due = today.withDayOfMonth(day);
                if (due.isBefore(today)) {
                    due = due.plusMonths(1);
                }
                Expense expense = Expense.create(
                        space.getId(),
                        bill.name(),
                        bill.amount(),
                        due,
                        bill.category() != null ? bill.category() : "Otros",
                        user.getFullName(),
                        RecurrenceType.RECURRING,
                        Frequency.MONTHLY,
                        null,
                        "Creado en onboarding",
                        userId
                );
                expenseRepository.save(expense);
            }
        }

        return space;
    }

    private Frequency parseFrequency(String value) {
        if (value == null || value.isBlank()) {
            return Frequency.MONTHLY;
        }
        return Frequency.valueOf(value.toUpperCase());
    }
}
