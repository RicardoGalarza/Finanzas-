package cl.flujoclaro.adapters.out.persistence;

import cl.flujoclaro.adapters.out.persistence.entity.FinancialSpaceEntity;
import cl.flujoclaro.adapters.out.persistence.entity.MembershipEntity;
import cl.flujoclaro.adapters.out.persistence.repository.MembershipPanacheRepository;
import cl.flujoclaro.adapters.out.persistence.repository.SpacePanacheRepository;
import cl.flujoclaro.domain.enums.MembershipRole;
import cl.flujoclaro.domain.enums.SpaceType;
import cl.flujoclaro.domain.model.FinancialSpace;
import cl.flujoclaro.domain.model.Membership;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SpaceRepositoryAdapter implements SpaceRepositoryPort {

    private final SpacePanacheRepository spaceRepository;
    private final MembershipPanacheRepository membershipRepository;

    public SpaceRepositoryAdapter(SpacePanacheRepository spaceRepository,
                                  MembershipPanacheRepository membershipRepository) {
        this.spaceRepository = spaceRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional
    public FinancialSpace save(FinancialSpace space) {
        FinancialSpaceEntity entity = spaceRepository.findByIdOptional(space.getId()).orElseGet(FinancialSpaceEntity::new);
        entity.id = space.getId();
        entity.name = space.getName();
        entity.type = space.getType().name();
        entity.currencyCode = space.getCurrencyCode();
        entity.initialBalance = space.getInitialBalance();
        entity.createdBy = space.getCreatedBy();
        entity.createdAt = space.getCreatedAt();
        entity.updatedAt = space.getUpdatedAt();
        spaceRepository.persist(entity);
        return toSpace(entity);
    }

    @Override
    public Optional<FinancialSpace> findById(UUID id) {
        return spaceRepository.findByIdOptional(id).map(this::toSpace);
    }

    @Override
    @Transactional
    public Membership saveMembership(Membership membership) {
        MembershipEntity entity = membershipRepository.findByIdOptional(membership.getId()).orElseGet(MembershipEntity::new);
        entity.id = membership.getId();
        entity.spaceId = membership.getSpaceId();
        entity.userId = membership.getUserId();
        entity.role = membership.getRole().name();
        entity.createdAt = membership.getCreatedAt();
        entity.updatedAt = membership.getUpdatedAt();
        membershipRepository.persist(entity);
        return toMembership(entity);
    }

    @Override
    public Optional<Membership> findMembership(UUID spaceId, UUID userId) {
        return membershipRepository.findBySpaceAndUser(spaceId, userId).map(this::toMembership);
    }

    @Override
    public List<Membership> findMembershipsByUser(UUID userId) {
        return membershipRepository.findByUser(userId).stream().map(this::toMembership).toList();
    }

    @Override
    public List<Membership> findMembershipsBySpace(UUID spaceId) {
        return membershipRepository.findBySpace(spaceId).stream().map(this::toMembership).toList();
    }

    private FinancialSpace toSpace(FinancialSpaceEntity entity) {
        FinancialSpace space = new FinancialSpace();
        space.setId(entity.id);
        space.setName(entity.name);
        space.setType(SpaceType.valueOf(entity.type));
        space.setCurrencyCode(entity.currencyCode);
        space.setInitialBalance(entity.initialBalance);
        space.setCreatedBy(entity.createdBy);
        space.setCreatedAt(entity.createdAt);
        space.setUpdatedAt(entity.updatedAt);
        return space;
    }

    private Membership toMembership(MembershipEntity entity) {
        Membership membership = new Membership();
        membership.setId(entity.id);
        membership.setSpaceId(entity.spaceId);
        membership.setUserId(entity.userId);
        membership.setRole(MembershipRole.valueOf(entity.role));
        membership.setCreatedAt(entity.createdAt);
        membership.setUpdatedAt(entity.updatedAt);
        return membership;
    }
}
