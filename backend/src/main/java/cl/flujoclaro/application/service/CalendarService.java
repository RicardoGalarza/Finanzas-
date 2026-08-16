package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.model.Income;
import cl.flujoclaro.domain.port.ExpenseRepositoryPort;
import cl.flujoclaro.domain.port.IncomeRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CalendarService {

    public record CalendarEvent(
            UUID id,
            String type,
            String title,
            BigDecimal amount,
            LocalDate date,
            String status,
            String category,
            boolean recurring
    ) {}

    public record DayDetail(LocalDate date, List<CalendarEvent> events) {}

    private final SpaceAccessService accessService;
    private final IncomeRepositoryPort incomeRepository;
    private final ExpenseRepositoryPort expenseRepository;

    public CalendarService(SpaceAccessService accessService,
                           IncomeRepositoryPort incomeRepository,
                           ExpenseRepositoryPort expenseRepository) {
        this.accessService = accessService;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<CalendarEvent> monthEvents(UUID spaceId, UUID userId, int year, int month) {
        accessService.requireMembership(spaceId, userId);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<CalendarEvent> events = new ArrayList<>();
        for (Income income : incomeRepository.findBySpaceBetween(spaceId, from, to)) {
            events.add(new CalendarEvent(
                    income.getId(),
                    "INCOME",
                    income.getDescription(),
                    income.getAmount(),
                    income.getIncomeDate(),
                    "RECEIVED",
                    income.getCategory(),
                    income.getIncomeType().name().equals("RECURRING")
            ));
        }
        for (Expense expense : expenseRepository.findBySpaceBetween(spaceId, from, to)) {
            ExpenseStatus status = expense.getStatus() == ExpenseStatus.PAID
                    ? ExpenseStatus.PAID
                    : expense.effectiveStatus(today);
            events.add(new CalendarEvent(
                    expense.getId(),
                    "EXPENSE",
                    expense.getName(),
                    expense.getAmount(),
                    expense.getDueDate(),
                    status.name(),
                    expense.getCategory(),
                    expense.getExpenseType().name().equals("RECURRING")
            ));
        }
        events.sort(Comparator.comparing(CalendarEvent::date).thenComparing(CalendarEvent::title));
        return events;
    }

    public DayDetail dayDetail(UUID spaceId, UUID userId, LocalDate date) {
        List<CalendarEvent> events = monthEvents(spaceId, userId, date.getYear(), date.getMonthValue()).stream()
                .filter(e -> e.date().equals(date))
                .toList();
        return new DayDetail(date, events);
    }
}
