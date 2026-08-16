package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.model.Income;
import cl.flujoclaro.domain.port.IncomeRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class IncomeService {

    public record IncomeCommand(
            String description,
            BigDecimal amount,
            LocalDate incomeDate,
            String category,
            String receivedBy,
            RecurrenceType incomeType,
            Frequency frequency,
            String paymentMethod,
            String notes
    ) {}

    private final IncomeRepositoryPort incomeRepository;
    private final SpaceAccessService accessService;

    public IncomeService(IncomeRepositoryPort incomeRepository, SpaceAccessService accessService) {
        this.incomeRepository = incomeRepository;
        this.accessService = accessService;
    }

    @Transactional
    public Income create(UUID spaceId, UUID userId, IncomeCommand command) {
        accessService.requireWriteAccess(spaceId, userId);
        Income income = Income.create(
                spaceId,
                command.description(),
                command.amount(),
                command.incomeDate(),
                command.category(),
                command.receivedBy(),
                command.incomeType(),
                command.frequency(),
                command.paymentMethod(),
                command.notes(),
                userId
        );
        return incomeRepository.save(income);
    }

    @Transactional
    public Income update(UUID spaceId, UUID incomeId, UUID userId, IncomeCommand command) {
        accessService.requireWriteAccess(spaceId, userId);
        Income income = getOwned(spaceId, incomeId);
        income.update(
                command.description(),
                command.amount(),
                command.incomeDate(),
                command.category(),
                command.receivedBy(),
                command.incomeType(),
                command.frequency(),
                command.paymentMethod(),
                command.notes(),
                userId
        );
        return incomeRepository.save(income);
    }

    public List<Income> list(UUID spaceId, UUID userId, String search, String category,
                             LocalDate from, LocalDate to) {
        accessService.requireMembership(spaceId, userId);
        return incomeRepository.findBySpace(spaceId, search, category, from, to);
    }

    public Income get(UUID spaceId, UUID incomeId, UUID userId) {
        accessService.requireMembership(spaceId, userId);
        return getOwned(spaceId, incomeId);
    }

    @Transactional
    public void delete(UUID spaceId, UUID incomeId, UUID userId) {
        accessService.requireWriteAccess(spaceId, userId);
        getOwned(spaceId, incomeId);
        incomeRepository.delete(incomeId);
    }

    private Income getOwned(UUID spaceId, UUID incomeId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new NotFoundException("Ingreso no encontrado"));
        if (!income.getSpaceId().equals(spaceId)) {
            throw new NotFoundException("Ingreso no encontrado");
        }
        return income;
    }
}
