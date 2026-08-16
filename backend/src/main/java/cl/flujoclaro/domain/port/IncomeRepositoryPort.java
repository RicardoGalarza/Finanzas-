package cl.flujoclaro.domain.port;

import cl.flujoclaro.domain.model.Income;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepositoryPort {
    Income save(Income income);
    Optional<Income> findById(UUID id);
    List<Income> findBySpace(UUID spaceId, String search, String category, LocalDate from, LocalDate to);
    List<Income> findBySpaceBetween(UUID spaceId, LocalDate from, LocalDate to);
    void delete(UUID id);
}
