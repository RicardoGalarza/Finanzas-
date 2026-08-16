package cl.flujoclaro.adapters.out.persistence.repository;

import cl.flujoclaro.adapters.out.persistence.entity.ExpenseEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ExpensePanacheRepository implements PanacheRepositoryBase<ExpenseEntity, UUID> {

    public List<ExpenseEntity> search(UUID spaceId, String search, String category, String status,
                                      LocalDate from, LocalDate to) {
        StringBuilder query = new StringBuilder("spaceId = :spaceId");
        Map<String, Object> params = new HashMap<>();
        params.put("spaceId", spaceId);

        if (search != null && !search.isBlank()) {
            query.append(" and lower(name) like :search");
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        if (category != null && !category.isBlank()) {
            query.append(" and category = :category");
            params.put("category", category);
        }
        if (status != null && !status.isBlank() && !"OVERDUE".equals(status)) {
            query.append(" and status = :status");
            params.put("status", status);
        }
        if (from != null) {
            query.append(" and dueDate >= :from");
            params.put("from", from);
        }
        if (to != null) {
            query.append(" and dueDate <= :to");
            params.put("to", to);
        }
        query.append(" order by dueDate asc");
        return list(query.toString(), params);
    }

    public List<ExpenseEntity> between(UUID spaceId, LocalDate from, LocalDate to) {
        return list("spaceId = ?1 and dueDate >= ?2 and dueDate <= ?3 order by dueDate", spaceId, from, to);
    }

    public List<ExpenseEntity> allBySpace(UUID spaceId) {
        return list("spaceId", spaceId);
    }
}
