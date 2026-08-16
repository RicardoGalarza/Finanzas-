package cl.flujoclaro.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class ExpenseEntity {
    @Id
    public UUID id;
    @Column(name = "space_id", nullable = false)
    public UUID spaceId;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public BigDecimal amount;
    @Column(name = "due_date", nullable = false)
    public LocalDate dueDate;
    @Column(nullable = false)
    public String category;
    @Column(name = "responsible_person", nullable = false)
    public String responsiblePerson;
    @Column(nullable = false)
    public String status;
    @Column(name = "expense_type", nullable = false)
    public String expenseType;
    public String frequency;
    @Column(name = "recurrence_end_date")
    public LocalDate recurrenceEndDate;
    @Column(name = "payment_method")
    public String paymentMethod;
    @Column(name = "receipt_path")
    public String receiptPath;
    public String notes;
    @Column(name = "paid_at")
    public LocalDate paidAt;
    @Column(name = "created_by", nullable = false)
    public UUID createdBy;
    @Column(name = "updated_by")
    public UUID updatedBy;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
