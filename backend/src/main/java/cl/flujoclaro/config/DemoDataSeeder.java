package cl.flujoclaro.config;

import cl.flujoclaro.application.service.AuthService;
import cl.flujoclaro.application.service.ExpenseService;
import cl.flujoclaro.application.service.IncomeService;
import cl.flujoclaro.application.service.OnboardingService;
import cl.flujoclaro.domain.enums.Frequency;
import cl.flujoclaro.domain.enums.RecurrenceType;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import cl.flujoclaro.domain.port.UserRepositoryPort;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DemoDataSeeder {

    private static final Logger LOG = Logger.getLogger(DemoDataSeeder.class);

    @ConfigProperty(name = "app.demo.enabled", defaultValue = "false")
    boolean demoEnabled;

    @ConfigProperty(name = "app.demo.email")
    String demoEmail;

    @ConfigProperty(name = "app.demo.password")
    String demoPassword;

    private final UserRepositoryPort userRepository;
    private final AuthService authService;
    private final OnboardingService onboardingService;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final SpaceRepositoryPort spaceRepository;

    public DemoDataSeeder(UserRepositoryPort userRepository,
                          AuthService authService,
                          OnboardingService onboardingService,
                          IncomeService incomeService,
                          ExpenseService expenseService,
                          SpaceRepositoryPort spaceRepository) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.onboardingService = onboardingService;
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.spaceRepository = spaceRepository;
    }

    void onStart(@Observes StartupEvent event) {
        seed();
    }

    @Transactional
    void seed() {
        if (!demoEnabled) {
            LOG.info("Seed demo deshabilitado (app.demo.enabled=false)");
            return;
        }
        if (userRepository.existsByEmail(demoEmail)) {
            LOG.info("Cuenta demo ya existe, no se regeneran datos");
            return;
        }
        LOG.info("Creando cuenta demo de FlujoClaro");
        var auth = authService.register("Usuario Demo", demoEmail, demoPassword);
        UUID userId = auth.userId();
        onboardingService.complete(userId, new OnboardingService.OnboardingCommand(
                "Usuario Demo",
                "Chile",
                "CLP",
                new BigDecimal("450000"),
                false,
                "Espacio Demo",
                List.of(new OnboardingService.HabitualIncome("Sueldo", new BigDecimal("1200000"), "Sueldo", "MONTHLY")),
                List.of(
                        new OnboardingService.HabitualBill("Arriendo", new BigDecimal("450000"), "Arriendo o dividendo", 5),
                        new OnboardingService.HabitualBill("Internet", new BigDecimal("35000"), "Internet", 10)
                )
        ));

        UUID spaceId = spaceRepository.findMembershipsByUser(userId).getFirst().getSpaceId();
        LocalDate today = LocalDate.now();
        incomeService.create(spaceId, userId, new IncomeService.IncomeCommand(
                "Bono productividad",
                new BigDecimal("150000"),
                today.minusDays(3),
                "Bonos",
                "Usuario Demo",
                RecurrenceType.ONE_TIME,
                null,
                "Transferencia",
                "Dato demo"
        ));
        expenseService.create(spaceId, userId, new ExpenseService.ExpenseCommand(
                "Supermercado",
                new BigDecimal("85000"),
                today.plusDays(2),
                "Supermercado",
                "Usuario Demo",
                RecurrenceType.ONE_TIME,
                null,
                "Tarjeta",
                "Dato demo"
        ));
        expenseService.create(spaceId, userId, new ExpenseService.ExpenseCommand(
                "Electricidad",
                new BigDecimal("42000"),
                today.minusDays(2),
                "Electricidad",
                "Usuario Demo",
                RecurrenceType.RECURRING,
                Frequency.MONTHLY,
                null,
                "Vencida demo"
        ));
        LOG.infof("Demo lista: %s / %s", demoEmail, demoPassword);
    }
}
