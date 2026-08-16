package cl.flujoclaro.domain.port;

import cl.flujoclaro.domain.model.Expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepositoryPort {
    Expense save(Expense expense);
    Optional<Expense> findById(UUID id);
    List<Expense> findBySpace(UUID spaceId, String search, String category, String status,
                              LocalDate from, LocalDate to);
    List<Expense> findBySpaceBetween(UUID spaceId, LocalDate from, LocalDate to);
    List<Expense> findAllBySpace(UUID spaceId);
    void delete(UUID id);
}
