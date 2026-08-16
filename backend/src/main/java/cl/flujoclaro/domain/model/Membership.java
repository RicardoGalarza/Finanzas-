package cl.flujoclaro.domain.model;

import cl.flujoclaro.domain.enums.MembershipRole;

import java.time.Instant;
import java.util.UUID;

public class Membership {
    private UUID id;
    private UUID spaceId;
    private UUID userId;
    private MembershipRole role;
    private Instant createdAt;
    private Instant updatedAt;

    public static Membership create(UUID spaceId, UUID userId, MembershipRole role) {
        Membership membership = new Membership();
        membership.id = UUID.randomUUID();
        membership.spaceId = spaceId;
        membership.userId = userId;
        membership.role = role;
        Instant now = Instant.now();
        membership.createdAt = now;
        membership.updatedAt = now;
        return membership;
    }

    public boolean canWrite() {
        return role == MembershipRole.ADMIN || role == MembershipRole.COLLABORATOR;
    }

    public boolean canManageMembers() {
        return role == MembershipRole.ADMIN;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public MembershipRole getRole() { return role; }
    public void setRole(MembershipRole role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
