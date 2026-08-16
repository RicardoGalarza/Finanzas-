package cl.flujoclaro.domain.port;

import cl.flujoclaro.domain.model.FinancialSpace;
import cl.flujoclaro.domain.model.Membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepositoryPort {
    FinancialSpace save(FinancialSpace space);
    Optional<FinancialSpace> findById(UUID id);
    Membership saveMembership(Membership membership);
    Optional<Membership> findMembership(UUID spaceId, UUID userId);
    List<Membership> findMembershipsByUser(UUID userId);
    List<Membership> findMembershipsBySpace(UUID spaceId);
}
