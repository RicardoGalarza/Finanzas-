package cl.flujoclaro.domain.model;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.exception.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Expense {
    private UUID id;
    private UUID spaceId;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String category;
    private String responsiblePerson;
    private ExpenseStatus status;
    private RecurrenceType expenseType;
    private Frequency frequency;
    private String paymentMethod;
    private String receiptPath;
    private String notes;
    private LocalDate paidAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static Expense create(UUID spaceId, String name, BigDecimal amount, LocalDate dueDate,
                                 String category, String responsiblePerson, RecurrenceType expenseType,
                                 Frequency frequency, String paymentMethod, String notes, UUID actorId) {
        validateAmount(amount);
        if (expenseType == RecurrenceType.RECURRING && frequency == null) {
            throw new DomainException("La frecuencia es obligatoria para gastos recurrentes");
        }
        Expense expense = new Expense();
        expense.id = UUID.randomUUID();
        expense.spaceId = spaceId;
        expense.name = name.trim();
        expense.amount = amount;
        expense.dueDate = dueDate;
        expense.category = category;
        expense.responsiblePerson = responsiblePerson;
        expense.status = ExpenseStatus.PENDING;
        expense.expenseType = expenseType;
        expense.frequency = frequency;
        expense.paymentMethod = paymentMethod;
        expense.notes = notes;
        expense.createdBy = actorId;
        expense.updatedBy = actorId;
        Instant now = Instant.now();
        expense.createdAt = now;
        expense.updatedAt = now;
        return expense;
    }

    public void update(String name, BigDecimal amount, LocalDate dueDate, String category,
                       String responsiblePerson, RecurrenceType expenseType, Frequency frequency,
                       String paymentMethod, String notes, UUID actorId) {
        validateAmount(amount);
        if (expenseType == RecurrenceType.RECURRING && frequency == null) {
            throw new DomainException("La frecuencia es obligatoria para gastos recurrentes");
        }
        this.name = name.trim();
        this.amount = amount;
        this.dueDate = dueDate;
        this.category = category;
        this.responsiblePerson = responsiblePerson;
        this.expenseType = expenseType;
        this.frequency = frequency;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.updatedBy = actorId;
        this.updatedAt = Instant.now();
        if (this.status != ExpenseStatus.PAID) {
            this.status = ExpenseStatus.PENDING;
        }
    }

    public void markPaid(LocalDate paidAt, UUID actorId) {
        this.status = ExpenseStatus.PAID;
        this.paidAt = paidAt != null ? paidAt : LocalDate.now();
        this.updatedBy = actorId;
        this.updatedAt = Instant.now();
    }

    public void attachReceipt(String receiptPath, UUID actorId) {
        if (receiptPath == null || receiptPath.isBlank()) {
            throw new DomainException("La ruta del comprobante es inválida");
        }
        this.receiptPath = receiptPath;
        this.updatedBy = actorId;
        this.updatedAt = Instant.now();
    }

    public ExpenseStatus effectiveStatus(LocalDate today) {
        return ExpenseStatus.effective(status, dueDate, today);
    }

    public boolean isPendingObligation(LocalDate today) {
        ExpenseStatus effective = effectiveStatus(today);
        return effective == ExpenseStatus.PENDING || effective == ExpenseStatus.OVERDUE;
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("El monto debe ser mayor a cero");
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }
    public RecurrenceType getExpenseType() { return expenseType; }
    public void setExpenseType(RecurrenceType expenseType) { this.expenseType = expenseType; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDate paidAt) { this.paidAt = paidAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
