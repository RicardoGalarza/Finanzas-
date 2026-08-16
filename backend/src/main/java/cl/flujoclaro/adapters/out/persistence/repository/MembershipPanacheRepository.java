package cl.flujoclaro.adapters.out.persistence.repository;

import cl.flujoclaro.adapters.out.persistence.entity.MembershipEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MembershipPanacheRepository implements PanacheRepositoryBase<MembershipEntity, UUID> {
    public Optional<MembershipEntity> findBySpaceAndUser(UUID spaceId, UUID userId) {
        return find("spaceId = ?1 and userId = ?2", spaceId, userId).firstResultOptional();
    }

    public List<MembershipEntity> findByUser(UUID userId) {
        return list("userId", userId);
    }

    public List<MembershipEntity> findBySpace(UUID spaceId) {
        return list("spaceId", spaceId);
    }
}
