package cl.flujoclaro.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    public UUID id;
    @Column(nullable = false, unique = true)
    public String email;
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;
    @Column(name = "full_name", nullable = false)
    public String fullName;
    public String country;
    @Column(name = "currency_code", nullable = false)
    public String currencyCode;
    @Column(name = "onboarding_completed", nullable = false)
    public boolean onboardingCompleted;
    @Column(name = "avatar_path")
    public String avatarPath;
    @Column(name = "reminder_days", nullable = false)
    public int reminderDays;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
