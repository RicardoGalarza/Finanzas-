package cl.flujoclaro.adapters.out.persistence;

import cl.flujoclaro.adapters.out.persistence.entity.UserEntity;
import cl.flujoclaro.adapters.out.persistence.repository.UserPanacheRepository;
import cl.flujoclaro.domain.model.User;
import cl.flujoclaro.domain.port.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserPanacheRepository repository;

    public UserRepositoryAdapter(UserPanacheRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = repository.findByIdOptional(user.getId()).orElseGet(UserEntity::new);
        entity.id = user.getId();
        entity.email = user.getEmail();
        entity.passwordHash = user.getPasswordHash();
        entity.fullName = user.getFullName();
        entity.country = user.getCountry();
        entity.currencyCode = user.getCurrencyCode();
        entity.onboardingCompleted = user.isOnboardingCompleted();
        entity.avatarPath = user.getAvatarPath();
        entity.reminderDays = user.getReminderDays();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        repository.persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findByIdOptional(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email.trim().toLowerCase()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.findByEmail(email.trim().toLowerCase()).isPresent();
    }

    private User toDomain(UserEntity entity) {
        User user = new User();
        user.setId(entity.id);
        user.setEmail(entity.email);
        user.setPasswordHash(entity.passwordHash);
        user.setFullName(entity.fullName);
        user.setCountry(entity.country);
        user.setCurrencyCode(entity.currencyCode);
        user.setOnboardingCompleted(entity.onboardingCompleted);
        user.setAvatarPath(entity.avatarPath);
        user.setReminderDays(entity.reminderDays);
        user.setCreatedAt(entity.createdAt);
        user.setUpdatedAt(entity.updatedAt);
        return user;
    }
}
