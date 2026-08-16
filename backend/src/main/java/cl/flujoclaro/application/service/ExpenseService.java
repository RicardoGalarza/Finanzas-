package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.port.ExpenseRepositoryPort;
import cl.flujoclaro.domain.port.ReceiptStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExpenseService {

    public record ExpenseCommand(
            String name,
            BigDecimal amount,
            LocalDate dueDate,
            String category,
            String responsiblePerson,
            RecurrenceType expenseType,
            Frequency frequency,
            LocalDate recurrenceEndDate,
            String paymentMethod,
            String notes
    ) {}

    private final ExpenseRepositoryPort expenseRepository;
    private final SpaceAccessService accessService;
    private final ReceiptStoragePort receiptStorage;

    public ExpenseService(ExpenseRepositoryPort expenseRepository,
                          SpaceAccessService accessService,
                          ReceiptStoragePort receiptStorage) {
        this.expenseRepository = expenseRepository;
        this.accessService = accessService;
        this.receiptStorage = receiptStorage;
    }

    @Transactional
    public Expense create(UUID spaceId, UUID userId, ExpenseCommand command) {
        accessService.requireWriteAccess(spaceId, userId);
        Expense expense = Expense.create(
                spaceId,
                command.name(),
                command.amount(),
                command.dueDate(),
                command.category(),
                command.responsiblePerson(),
                command.expenseType(),
                command.frequency(),
                command.recurrenceEndDate(),
                command.paymentMethod(),
                command.notes(),
                userId
        );
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense update(UUID spaceId, UUID expenseId, UUID userId, ExpenseCommand command) {
        accessService.requireWriteAccess(spaceId, userId);
        Expense expense = getOwned(spaceId, expenseId);
        expense.update(
                command.name(),
                command.amount(),
                command.dueDate(),
                command.category(),
                command.responsiblePerson(),
                command.expenseType(),
                command.frequency(),
                command.recurrenceEndDate(),
                command.paymentMethod(),
                command.notes(),
                userId
        );
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense markPaid(UUID spaceId, UUID expenseId, UUID userId, LocalDate paidAt, String receiptPath) {
        return markPaid(spaceId, expenseId, userId, paidAt, null, receiptPath);
    }

    @Transactional
    public Expense markPaid(UUID spaceId, UUID expenseId, UUID userId, LocalDate paidAt,
                            String paymentMethod, String receiptPath) {
        accessService.requireWriteAccess(spaceId, userId);
        Expense expense = getOwned(spaceId, expenseId);
        boolean firstPayment = expense.getStatus() != ExpenseStatus.PAID;
        expense.markPaid(paidAt, paymentMethod, userId);
        if (receiptPath != null) {
            if (expense.getReceiptPath() != null) {
                receiptStorage.delete(expense.getReceiptPath());
            }
            expense.attachReceipt(receiptPath, userId);
        }
        Expense paidExpense = expenseRepository.save(expense);
        if (firstPayment && expense.getExpenseType() == RecurrenceType.RECURRING) {
            LocalDate nextDueDate = expense.nextDueDate();
            if (nextDueDate != null && canCreateNextInstallment(expense, nextDueDate)) {
                Expense nextExpense = Expense.create(
                        spaceId,
                        expense.getName(),
                        expense.getAmount(),
                        nextDueDate,
                        expense.getCategory(),
                        expense.getResponsiblePerson(),
                        expense.getExpenseType(),
                        expense.getFrequency(),
                        expense.getRecurrenceEndDate(),
                        expense.getPaymentMethod(),
                        expense.getNotes(),
                        userId
                );
                expenseRepository.save(nextExpense);
            }
        }
        return paidExpense;
    }

    /**
     * Evita generar cuotas repetidas cuando ya existe la del próximo vencimiento
     * o cuando la serie todavía tiene una cuota sin pagar igual o posterior.
     */
    private boolean canCreateNextInstallment(Expense paid, LocalDate nextDueDate) {
        return expenseRepository.findAllBySpace(paid.getSpaceId()).stream()
                .filter(other -> !other.getId().equals(paid.getId()))
                .filter(other -> other.getName().equalsIgnoreCase(paid.getName()))
                .noneMatch(other -> other.getDueDate().isEqual(nextDueDate)
                        || (other.getStatus() != ExpenseStatus.PAID
                            && !other.getDueDate().isBefore(paid.getDueDate())));
    }

    @Transactional
    public Expense replaceReceipt(UUID spaceId, UUID expenseId, UUID userId, String receiptPath) {
        accessService.requireWriteAccess(spaceId, userId);
        Expense expense = getOwned(spaceId, expenseId);
        String previousReceipt = expense.getReceiptPath();
        expense.attachReceipt(receiptPath, userId);
        Expense saved = expenseRepository.save(expense);
        if (previousReceipt != null) {
            receiptStorage.delete(previousReceipt);
        }
        return saved;
    }

    public ReceiptStoragePort.StoredReceipt getReceipt(UUID spaceId, UUID expenseId, UUID userId) {
        accessService.requireMembership(spaceId, userId);
        Expense expense = getOwned(spaceId, expenseId);
        if (expense.getReceiptPath() == null) {
            throw new NotFoundException("Esta cuenta no tiene comprobante");
        }
        return receiptStorage.load(expense.getReceiptPath());
    }

    public List<Expense> list(UUID spaceId, UUID userId, String search, String category, String status,
                              LocalDate from, LocalDate to) {
        accessService.requireMembership(spaceId, userId);
        LocalDate today = LocalDate.now();
        return expenseRepository.findBySpace(spaceId, search, category, status, from, to).stream()
                .peek(e -> {
                    if (e.getStatus() != ExpenseStatus.PAID) {
                        e.setStatus(e.effectiveStatus(today));
                    }
                })
                .toList();
    }

    public Expense get(UUID spaceId, UUID expenseId, UUID userId) {
        accessService.requireMembership(spaceId, userId);
        Expense expense = getOwned(spaceId, expenseId);
        if (expense.getStatus() != ExpenseStatus.PAID) {
            expense.setStatus(expense.effectiveStatus(LocalDate.now()));
        }
        return expense;
    }

    @Transactional
    public void delete(UUID spaceId, UUID expenseId, UUID userId) {
        accessService.requireWriteAccess(spaceId, userId);
        Expense expense = getOwned(spaceId, expenseId);
        if (expense.getReceiptPath() != null) {
            receiptStorage.delete(expense.getReceiptPath());
        }
        expenseRepository.delete(expenseId);
    }

    private Expense getOwned(UUID spaceId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("Gasto/cuenta no encontrado"));
        if (!expense.getSpaceId().equals(spaceId)) {
            throw new NotFoundException("Gasto/cuenta no encontrado");
        }
        return expense;
    }
}
