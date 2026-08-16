package cl.flujoclaro.adapters.out.persistence;

import cl.flujoclaro.adapters.out.persistence.entity.IncomeEntity;
import cl.flujoclaro.adapters.out.persistence.repository.IncomePanacheRepository;
import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.model.Income;
import cl.flujoclaro.domain.port.IncomeRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IncomeRepositoryAdapter implements IncomeRepositoryPort {

    private final IncomePanacheRepository repository;

    public IncomeRepositoryAdapter(IncomePanacheRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Income save(Income income) {
        IncomeEntity entity = repository.findByIdOptional(income.getId()).orElseGet(IncomeEntity::new);
        mapToEntity(income, entity);
        repository.persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Income> findById(UUID id) {
        return repository.findByIdOptional(id).map(this::toDomain);
    }

    @Override
    public List<Income> findBySpace(UUID spaceId, String search, String category, LocalDate from, LocalDate to) {
        return repository.search(spaceId, search, category, from, to).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Income> findBySpaceBetween(UUID spaceId, LocalDate from, LocalDate to) {
        return repository.between(spaceId, from, to).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private void mapToEntity(Income income, IncomeEntity entity) {
        entity.id = income.getId();
        entity.spaceId = income.getSpaceId();
        entity.description = income.getDescription();
        entity.amount = income.getAmount();
        entity.incomeDate = income.getIncomeDate();
        entity.category = income.getCategory();
        entity.receivedBy = income.getReceivedBy();
        entity.incomeType = income.getIncomeType().name();
        entity.frequency = income.getFrequency() != null ? income.getFrequency().name() : null;
        entity.paymentMethod = income.getPaymentMethod();
        entity.notes = income.getNotes();
        entity.createdBy = income.getCreatedBy();
        entity.updatedBy = income.getUpdatedBy();
        entity.createdAt = income.getCreatedAt();
        entity.updatedAt = income.getUpdatedAt();
    }

    private Income toDomain(IncomeEntity entity) {
        Income income = new Income();
        income.setId(entity.id);
        income.setSpaceId(entity.spaceId);
        income.setDescription(entity.description);
        income.setAmount(entity.amount);
        income.setIncomeDate(entity.incomeDate);
        income.setCategory(entity.category);
        income.setReceivedBy(entity.receivedBy);
        income.setIncomeType(RecurrenceType.valueOf(entity.incomeType));
        income.setFrequency(entity.frequency != null ? Frequency.valueOf(entity.frequency) : null);
        income.setPaymentMethod(entity.paymentMethod);
        income.setNotes(entity.notes);
        income.setCreatedBy(entity.createdBy);
        income.setUpdatedBy(entity.updatedBy);
        income.setCreatedAt(entity.createdAt);
        income.setUpdatedAt(entity.updatedAt);
        return income;
    }
}
