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
@Table(name = "incomes")
public class IncomeEntity {
    @Id
    public UUID id;
    @Column(name = "space_id", nullable = false)
    public UUID spaceId;
    @Column(nullable = false)
    public String description;
    @Column(nullable = false)
    public BigDecimal amount;
    @Column(name = "income_date", nullable = false)
    public LocalDate incomeDate;
    @Column(nullable = false)
    public String category;
    @Column(name = "received_by", nullable = false)
    public String receivedBy;
    @Column(name = "income_type", nullable = false)
    public String incomeType;
    public String frequency;
    @Column(name = "payment_method")
    public String paymentMethod;
    public String notes;
    @Column(name = "created_by", nullable = false)
    public UUID createdBy;
    @Column(name = "updated_by")
    public UUID updatedBy;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
