package cl.flujoclaro.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_spaces")
public class FinancialSpaceEntity {
    @Id
    public UUID id;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public String type;
    @Column(name = "currency_code", nullable = false)
    public String currencyCode;
    @Column(name = "initial_balance", nullable = false)
    public BigDecimal initialBalance;
    @Column(name = "created_by", nullable = false)
    public UUID createdBy;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
