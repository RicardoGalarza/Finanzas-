package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.MembershipRole;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.exception.UnauthorizedException;
import cl.flujoclaro.domain.model.Membership;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class SpaceAccessService {

    private final SpaceRepositoryPort spaceRepository;

    public SpaceAccessService(SpaceRepositoryPort spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public Membership requireMembership(UUID spaceId, UUID userId) {
        return spaceRepository.findMembership(spaceId, userId)
                .orElseThrow(() -> new UnauthorizedException("No tienes acceso a este espacio financiero"));
    }

    public Membership requireWriteAccess(UUID spaceId, UUID userId) {
        Membership membership = requireMembership(spaceId, userId);
        if (!membership.canWrite()) {
            throw new UnauthorizedException("Tu rol solo permite lectura");
        }
        return membership;
    }

    public void requireSpaceExists(UUID spaceId) {
        spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NotFoundException("Espacio financiero no encontrado"));
    }

    public MembershipRole roleOf(UUID spaceId, UUID userId) {
        return requireMembership(spaceId, userId).getRole();
    }
}
