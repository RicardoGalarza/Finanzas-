package cl.flujoclaro.domain.model;

import cl.flujoclaro.domain.exception.DomainException;

import java.time.Instant;
import java.util.UUID;

public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String country;
    private String currencyCode;
    private boolean onboardingCompleted;
    private String avatarPath;
    private int reminderDays = 3;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
    }

    public static User create(String email, String passwordHash, String fullName) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = email.trim().toLowerCase();
        user.passwordHash = passwordHash;
        user.fullName = fullName.trim();
        user.currencyCode = "CLP";
        user.onboardingCompleted = false;
        Instant now = Instant.now();
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public void completeOnboarding(String fullName, String country, String currencyCode) {
        this.fullName = fullName.trim();
        this.country = country;
        this.currencyCode = currencyCode;
        this.onboardingCompleted = true;
        this.updatedAt = Instant.now();
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }

    public void updateAvatar(String avatarPath) {
        this.avatarPath = avatarPath;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String fullName, String country, String currencyCode, int reminderDays) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("El nombre es obligatorio");
        }
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new DomainException("La moneda es obligatoria");
        }
        if (reminderDays < 0 || reminderDays > 30) {
            throw new DomainException("Los días de anticipación deben estar entre 0 y 30");
        }
        this.fullName = fullName.trim();
        this.country = country == null ? "" : country.trim();
        this.currencyCode = currencyCode.trim().toUpperCase();
        this.reminderDays = reminderDays;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public int getReminderDays() { return reminderDays; }
    public void setReminderDays(int reminderDays) { this.reminderDays = reminderDays; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
