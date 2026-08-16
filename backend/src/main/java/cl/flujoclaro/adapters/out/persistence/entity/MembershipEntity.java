package cl.flujoclaro.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class MembershipEntity {
    @Id
    public UUID id;
    @Column(name = "space_id", nullable = false)
    public UUID spaceId;
    @Column(name = "user_id", nullable = false)
    public UUID userId;
    @Column(nullable = false)
    public String role;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
