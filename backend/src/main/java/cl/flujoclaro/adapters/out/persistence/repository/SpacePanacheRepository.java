package cl.flujoclaro.adapters.out.persistence.repository;

import cl.flujoclaro.adapters.out.persistence.entity.FinancialSpaceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class SpacePanacheRepository implements PanacheRepositoryBase<FinancialSpaceEntity, UUID> {
}
