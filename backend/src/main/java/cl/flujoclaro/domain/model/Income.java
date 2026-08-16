package cl.flujoclaro.domain.model;

import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.exception.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Income {
    private UUID id;
    private UUID spaceId;
    private String description;
    private BigDecimal amount;
    private LocalDate incomeDate;
    private String category;
    private String receivedBy;
    private RecurrenceType incomeType;
    private Frequency frequency;
    private String paymentMethod;
    private String notes;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static Income create(UUID spaceId, String description, BigDecimal amount, LocalDate incomeDate,
                                String category, String receivedBy, RecurrenceType incomeType,
                                Frequency frequency, String paymentMethod, String notes, UUID actorId) {
        validateAmount(amount);
        if (incomeType == RecurrenceType.RECURRING && frequency == null) {
            throw new DomainException("La frecuencia es obligatoria para ingresos recurrentes");
        }
        Income income = new Income();
        income.id = UUID.randomUUID();
        income.spaceId = spaceId;
        income.description = description.trim();
        income.amount = amount;
        income.incomeDate = incomeDate;
        income.category = category;
        income.receivedBy = receivedBy;
        income.incomeType = incomeType;
        income.frequency = frequency;
        income.paymentMethod = paymentMethod;
        income.notes = notes;
        income.createdBy = actorId;
        income.updatedBy = actorId;
        Instant now = Instant.now();
        income.createdAt = now;
        income.updatedAt = now;
        return income;
    }

    public void update(String description, BigDecimal amount, LocalDate incomeDate, String category,
                       String receivedBy, RecurrenceType incomeType, Frequency frequency,
                       String paymentMethod, String notes, UUID actorId) {
        validateAmount(amount);
        if (incomeType == RecurrenceType.RECURRING && frequency == null) {
            throw new DomainException("La frecuencia es obligatoria para ingresos recurrentes");
        }
        this.description = description.trim();
        this.amount = amount;
        this.incomeDate = incomeDate;
        this.category = category;
        this.receivedBy = receivedBy;
        this.incomeType = incomeType;
        this.frequency = frequency;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.updatedBy = actorId;
        this.updatedAt = Instant.now();
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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getIncomeDate() { return incomeDate; }
    public void setIncomeDate(LocalDate incomeDate) { this.incomeDate = incomeDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public RecurrenceType getIncomeType() { return incomeType; }
    public void setIncomeType(RecurrenceType incomeType) { this.incomeType = incomeType; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
