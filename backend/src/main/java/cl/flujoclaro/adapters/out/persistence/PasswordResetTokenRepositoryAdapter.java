package cl.flujoclaro.adapters.out.persistence;

import cl.flujoclaro.adapters.out.persistence.entity.PasswordResetTokenEntity;
import cl.flujoclaro.adapters.out.persistence.repository.PasswordResetTokenPanacheRepository;
import cl.flujoclaro.domain.model.PasswordResetToken;
import cl.flujoclaro.domain.port.PasswordResetTokenRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenPanacheRepository repository;

    public PasswordResetTokenRepositoryAdapter(PasswordResetTokenPanacheRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = repository.findByIdOptional(token.getId()).orElseGet(PasswordResetTokenEntity::new);
        entity.id = token.getId();
        entity.userId = token.getUserId();
        entity.tokenHash = token.getTokenHash();
        entity.expiresAt = token.getExpiresAt();
        entity.usedAt = token.getUsedAt();
        entity.createdAt = token.getCreatedAt();
        repository.persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    private PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(entity.id);
        token.setUserId(entity.userId);
        token.setTokenHash(entity.tokenHash);
        token.setExpiresAt(entity.expiresAt);
        token.setUsedAt(entity.usedAt);
        token.setCreatedAt(entity.createdAt);
        return token;
    }
}
