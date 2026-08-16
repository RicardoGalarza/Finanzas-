package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.model.FinancialSpace;
import cl.flujoclaro.domain.model.FinancialSummaryCalculator;
import cl.flujoclaro.domain.model.Income;
import cl.flujoclaro.domain.port.ExpenseRepositoryPort;
import cl.flujoclaro.domain.port.IncomeRepositoryPort;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import cl.flujoclaro.domain.port.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DashboardService {

    public record UpcomingItem(UUID id, String name, BigDecimal amount, LocalDate dueDate, String status, String category) {}
    public record CategorySlice(String category, BigDecimal amount) {}
    public record MonthlyPoint(String month, BigDecimal incomes, BigDecimal expenses) {}
    public record DashboardSummary(
            BigDecimal currentBalance,
            BigDecimal monthlyIncomes,
            BigDecimal monthlyPaidExpenses,
            BigDecimal pendingObligations,
            BigDecimal availableMoney,
            BigDecimal incomeUsagePercentage,
            List<UpcomingItem> upcoming,
            List<CategorySlice> expensesByCategory,
            List<MonthlyPoint> monthlyComparison
    ) {}

    private final SpaceAccessService accessService;
    private final SpaceRepositoryPort spaceRepository;
    private final IncomeRepositoryPort incomeRepository;
    private final ExpenseRepositoryPort expenseRepository;
    private final UserRepositoryPort userRepository;

    public DashboardService(SpaceAccessService accessService,
                            SpaceRepositoryPort spaceRepository,
                            IncomeRepositoryPort incomeRepository,
                            ExpenseRepositoryPort expenseRepository,
                            UserRepositoryPort userRepository) {
        this.accessService = accessService;
        this.spaceRepository = spaceRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public DashboardSummary getSummary(UUID spaceId, UUID userId) {
        accessService.requireMembership(spaceId, userId);
        FinancialSpace space = spaceRepository.findById(spaceId).orElseThrow();
        LocalDate today = LocalDate.now();
        int reminderDays = userRepository.findById(userId)
                .map(user -> user.getReminderDays())
                .orElse(3);
        LocalDate reminderLimit = today.plusDays(reminderDays);
        YearMonth current = YearMonth.from(today);

        List<Income> allIncomes = incomeRepository.findBySpace(spaceId, null, null, null, null);
        List<Expense> allExpenses = expenseRepository.findAllBySpace(spaceId);

        BigDecimal totalIncomes = allIncomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = allExpenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.PAID)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = allExpenses.stream()
                .filter(e -> e.isPendingObligation(today))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentBalance = FinancialSummaryCalculator.currentBalance(
                space.getInitialBalance(), totalIncomes, totalPaid);
        BigDecimal available = FinancialSummaryCalculator.availableMoney(currentBalance, pending);

        BigDecimal monthlyIncomes = allIncomes.stream()
                .filter(i -> YearMonth.from(i.getIncomeDate()).equals(current))
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyPaid = allExpenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.PAID)
                .filter(e -> e.getPaidAt() != null && YearMonth.from(e.getPaidAt()).equals(current)
                        || (e.getPaidAt() == null && YearMonth.from(e.getDueDate()).equals(current)))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UpcomingItem> upcoming = allExpenses.stream()
                .filter(e -> e.isPendingObligation(today))
                .filter(e -> e.getDueDate().isBefore(today) || !e.getDueDate().isAfter(reminderLimit))
                .sorted(Comparator.comparing(Expense::getDueDate))
                .limit(8)
                .map(e -> new UpcomingItem(
                        e.getId(),
                        e.getName(),
                        e.getAmount(),
                        e.getDueDate(),
                        e.effectiveStatus(today).name(),
                        e.getCategory()))
                .toList();

        Map<String, BigDecimal> byCategory = new HashMap<>();
        allExpenses.stream()
                .filter(e -> YearMonth.from(e.getDueDate()).equals(current))
                .forEach(e -> byCategory.merge(e.getCategory(), e.getAmount(), BigDecimal::add));
        List<CategorySlice> slices = byCategory.entrySet().stream()
                .map(e -> new CategorySlice(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategorySlice::amount).reversed())
                .toList();

        List<MonthlyPoint> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            BigDecimal incomes = allIncomes.stream()
                    .filter(inc -> YearMonth.from(inc.getIncomeDate()).equals(ym))
                    .map(Income::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expenses = allExpenses.stream()
                    .filter(exp -> YearMonth.from(exp.getDueDate()).equals(ym))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthly.add(new MonthlyPoint(ym.toString(), incomes, expenses));
        }

        return new DashboardSummary(
                currentBalance,
                monthlyIncomes,
                monthlyPaid,
                pending,
                available,
                FinancialSummaryCalculator.incomeUsagePercentage(monthlyIncomes, monthlyPaid),
                upcoming,
                slices,
                monthly
        );
    }
}
