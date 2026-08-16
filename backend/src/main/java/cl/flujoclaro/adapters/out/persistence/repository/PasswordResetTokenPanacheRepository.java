package cl.flujoclaro.adapters.out.persistence.repository;

import cl.flujoclaro.adapters.out.persistence.entity.PasswordResetTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PasswordResetTokenPanacheRepository implements PanacheRepositoryBase<PasswordResetTokenEntity, UUID> {
    public Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }
}
