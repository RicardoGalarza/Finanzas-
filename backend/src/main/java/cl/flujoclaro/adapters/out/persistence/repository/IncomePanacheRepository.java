package cl.flujoclaro.adapters.out.persistence.repository;

import cl.flujoclaro.adapters.out.persistence.entity.IncomeEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class IncomePanacheRepository implements PanacheRepositoryBase<IncomeEntity, UUID> {

    public List<IncomeEntity> search(UUID spaceId, String search, String category, LocalDate from, LocalDate to) {
        StringBuilder query = new StringBuilder("spaceId = :spaceId");
        Map<String, Object> params = new HashMap<>();
        params.put("spaceId", spaceId);

        if (search != null && !search.isBlank()) {
            query.append(" and lower(description) like :search");
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        if (category != null && !category.isBlank()) {
            query.append(" and category = :category");
            params.put("category", category);
        }
        if (from != null) {
            query.append(" and incomeDate >= :from");
            params.put("from", from);
        }
        if (to != null) {
            query.append(" and incomeDate <= :to");
            params.put("to", to);
        }
        query.append(" order by incomeDate desc");
        return list(query.toString(), params);
    }

    public List<IncomeEntity> between(UUID spaceId, LocalDate from, LocalDate to) {
        return list("spaceId = ?1 and incomeDate >= ?2 and incomeDate <= ?3 order by incomeDate", spaceId, from, to);
    }
}
