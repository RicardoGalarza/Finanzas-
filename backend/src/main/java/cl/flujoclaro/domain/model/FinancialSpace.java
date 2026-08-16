package cl.flujoclaro.domain.model;

import cl.flujoclaro.domain.enums.SpaceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FinancialSpace {
    private UUID id;
    private String name;
    private SpaceType type;
    private String currencyCode;
    private BigDecimal initialBalance;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static FinancialSpace create(String name, SpaceType type, String currencyCode,
                                        BigDecimal initialBalance, UUID createdBy) {
        FinancialSpace space = new FinancialSpace();
        space.id = UUID.randomUUID();
        space.name = name.trim();
        space.type = type;
        space.currencyCode = currencyCode;
        space.initialBalance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        space.createdBy = createdBy;
        Instant now = Instant.now();
        space.createdAt = now;
        space.updatedAt = now;
        return space;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SpaceType getType() { return type; }
    public void setType(SpaceType type) { this.type = type; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
