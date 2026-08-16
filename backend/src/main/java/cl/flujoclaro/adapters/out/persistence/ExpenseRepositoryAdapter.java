package cl.flujoclaro.adapters.out.persistence;

import cl.flujoclaro.adapters.out.persistence.entity.ExpenseEntity;
import cl.flujoclaro.adapters.out.persistence.repository.ExpensePanacheRepository;
import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.port.ExpenseRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExpenseRepositoryAdapter implements ExpenseRepositoryPort {

    private final ExpensePanacheRepository repository;

    public ExpenseRepositoryAdapter(ExpensePanacheRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Expense save(Expense expense) {
        ExpenseEntity entity = repository.findByIdOptional(expense.getId()).orElseGet(ExpenseEntity::new);
        mapToEntity(expense, entity);
        repository.persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        return repository.findByIdOptional(id).map(this::toDomain);
    }

    @Override
    public List<Expense> findBySpace(UUID spaceId, String search, String category, String status,
                                     LocalDate from, LocalDate to) {
        List<Expense> expenses = repository.search(spaceId, search, category, status, from, to)
                .stream().map(this::toDomain).toList();
        if ("OVERDUE".equalsIgnoreCase(status)) {
            LocalDate today = LocalDate.now();
            return expenses.stream()
                    .filter(e -> e.effectiveStatus(today) == ExpenseStatus.OVERDUE)
                    .toList();
        }
        return expenses;
    }

    @Override
    public List<Expense> findBySpaceBetween(UUID spaceId, LocalDate from, LocalDate to) {
        return repository.between(spaceId, from, to).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Expense> findAllBySpace(UUID spaceId) {
        return repository.allBySpace(spaceId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private void mapToEntity(Expense expense, ExpenseEntity entity) {
        entity.id = expense.getId();
        entity.spaceId = expense.getSpaceId();
        entity.name = expense.getName();
        entity.amount = expense.getAmount();
        entity.dueDate = expense.getDueDate();
        entity.category = expense.getCategory();
        entity.responsiblePerson = expense.getResponsiblePerson();
        entity.status = expense.getStatus().name();
        entity.expenseType = expense.getExpenseType().name();
        entity.frequency = expense.getFrequency() != null ? expense.getFrequency().name() : null;
        entity.recurrenceEndDate = expense.getRecurrenceEndDate();
        entity.paymentMethod = expense.getPaymentMethod();
        entity.receiptPath = expense.getReceiptPath();
        entity.notes = expense.getNotes();
        entity.paidAt = expense.getPaidAt();
        entity.createdBy = expense.getCreatedBy();
        entity.updatedBy = expense.getUpdatedBy();
        entity.createdAt = expense.getCreatedAt();
        entity.updatedAt = expense.getUpdatedAt();
    }

    private Expense toDomain(ExpenseEntity entity) {
        Expense expense = new Expense();
        expense.setId(entity.id);
        expense.setSpaceId(entity.spaceId);
        expense.setName(entity.name);
        expense.setAmount(entity.amount);
        expense.setDueDate(entity.dueDate);
        expense.setCategory(entity.category);
        expense.setResponsiblePerson(entity.responsiblePerson);
        expense.setStatus(ExpenseStatus.valueOf(entity.status));
        expense.setExpenseType(RecurrenceType.valueOf(entity.expenseType));
        expense.setFrequency(entity.frequency != null ? Frequency.valueOf(entity.frequency) : null);
        expense.setRecurrenceEndDate(entity.recurrenceEndDate);
        expense.setPaymentMethod(entity.paymentMethod);
        expense.setReceiptPath(entity.receiptPath);
        expense.setNotes(entity.notes);
        expense.setPaidAt(entity.paidAt);
        expense.setCreatedBy(entity.createdBy);
        expense.setUpdatedBy(entity.updatedBy);
        expense.setCreatedAt(entity.createdAt);
        expense.setUpdatedAt(entity.updatedAt);
        return expense;
    }
}
