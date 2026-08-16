package cl.flujoclaro.application.service;

import cl.flujoclaro.domain.enums.ExpenseStatus;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.model.FinancialSpace;
import cl.flujoclaro.domain.port.ExpenseRepositoryPort;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class FinancialAssistantService {

    public record AssistantReply(String reply, String intent, List<String> suggestions) {}

    private static final Locale CHILE = Locale.of("es", "CL");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Revisa mis finanzas",
            "¿Cuánto tengo disponible?",
            "¿Qué cuentas tengo pendientes?"
    );

    private final DashboardService dashboardService;
    private final ExpenseRepositoryPort expenseRepository;
    private final SpaceRepositoryPort spaceRepository;

    public FinancialAssistantService(DashboardService dashboardService,
                                     ExpenseRepositoryPort expenseRepository,
                                     SpaceRepositoryPort spaceRepository) {
        this.dashboardService = dashboardService;
        this.expenseRepository = expenseRepository;
        this.spaceRepository = spaceRepository;
    }

    public AssistantReply reply(UUID spaceId, UUID userId, String message) {
        DashboardService.DashboardSummary summary = dashboardService.getSummary(spaceId, userId);
        FinancialSpace space = spaceRepository.findById(spaceId).orElseThrow();
        String currency = space.getCurrencyCode();
        String text = normalize(message);

        AssistantReply smallTalk = smallTalkReply(text);
        if (smallTalk != null) {
            return smallTalk;
        }

        return switch (detectIntent(text)) {
            case "HELP" -> helpReply();
            case "REVIEW" -> reviewReply(spaceId, currency, summary, space.getName());
            case "BALANCE" -> balanceReply(currency, summary);
            case "INCOMES" -> incomesReply(currency, summary);
            case "PENDING" -> pendingReply(spaceId, currency);
            case "ADVICE" -> adviceReply(spaceId, currency, summary);
            case "EXPENSES" -> expenseReply(spaceId, text, currency, summary);
            default -> unknownReply();
        };
    }

    /**
     * Elige la intención con más coincidencias para tolerar frases naturales
     * como "revisa mis finanzas" o "cuánto me queda para gastar".
     */
    private String detectIntent(String text) {
        if (text.isBlank()) {
            return "HELP";
        }

        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, text, "HELP", "ayuda", "que puedes", "que sabes", "opciones", "que haces");
        addCandidate(candidates, text, "REVIEW", "revisa", "revisame", "revisar", "analiza", "analizar",
                "analisis", "como van", "como voy", "como estoy", "resumen", "resumeme", "mis finanzas",
                "mi situacion", "estado de mis", "como esta mi");
        addCandidate(candidates, text, "ADVICE", "consejo", "recomienda", "recomiendame", "que hago",
                "como ahorro", "puedo ahorrar", "gastando mucho", "sugerencia");
        addCandidate(candidates, text, "PENDING", "pendiente", "por pagar", "que debo", "debo pagar",
                "vencimiento", "vence", "vencida", "atrasad", "deuda", "proxima cuenta");
        addCandidate(candidates, text, "BALANCE", "disponible", "me queda", "puedo gastar", "saldo",
                "cuanto tengo", "cuanta plata");
        addCandidate(candidates, text, "INCOMES", "ingreso", "sueldo", "gane", "recibi", "entro plata");
        addCandidate(candidates, text, "EXPENSES", "gasto", "gaste", "gastos", "categoria", "en que se me va");

        return candidates.isEmpty() ? "UNKNOWN" : candidates.getFirst();
    }

    private void addCandidate(List<String> candidates, String text, String intent, String... terms) {
        if (containsAny(text, terms)) {
            candidates.add(intent);
        }
    }

    /** Responde saludos y cortesías antes de interpretar preguntas financieras. */
    private AssistantReply smallTalkReply(String message) {
        if (containsAny(message, "gracias", "te pasaste", "genial", "excelente")) {
            return response("¡De nada! Cuando quieras volvemos a revisar tus números.", "THANKS",
                    List.of("Revisa mis finanzas", "¿Tengo cuentas atrasadas?", "Dame un consejo"));
        }
        if (containsAny(message, "chao", "adios", "nos vemos", "hasta luego")) {
            return response("¡Hasta pronto! Aquí estaré cuando necesites revisar tus cuentas.", "GOODBYE",
                    DEFAULT_SUGGESTIONS);
        }
        if (containsAny(message, "como estas", "como te va", "todo bien")) {
            return response(
                    "¡Muy bien, gracias por preguntar! ¿Quieres que revise cómo van tus finanzas?",
                    "SMALL_TALK",
                    DEFAULT_SUGGESTIONS
            );
        }
        if (containsAny(message, "quien eres", "como te llamas", "que eres")) {
            return response(
                    "Soy el asistente de FlujoClaro. Reviso tu saldo, ingresos, gastos y vencimientos "
                            + "con la información que registras en la aplicación.",
                    "ABOUT",
                    DEFAULT_SUGGESTIONS
            );
        }
        if (startsWithAny(message, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches",
                "hey", "que tal", "holi")) {
            return response("¡Hola! ¿Quieres que revise cómo van tus finanzas?", "GREETING",
                    DEFAULT_SUGGESTIONS);
        }
        return null;
    }

    private AssistantReply helpReply() {
        return response(
                "Claro que sí. Puedo revisar tus finanzas completas, decirte cuánto tienes disponible, "
                        + "qué cuentas están pendientes o atrasadas, en qué se te va el dinero y darte un consejo. "
                        + "Pídemelo con tus palabras, por ejemplo: \"revisa mis finanzas\".",
                "HELP",
                DEFAULT_SUGGESTIONS
        );
    }

    private AssistantReply reviewReply(UUID spaceId, String currency,
                                       DashboardService.DashboardSummary summary, String spaceName) {
        LocalDate today = LocalDate.now();
        List<Expense> overdue = overdueExpenses(spaceId, today);

        StringBuilder reply = new StringBuilder();
        reply.append("Listo, revisé ").append(spaceName).append(". ");
        reply.append("Este mes llevas ").append(money(summary.monthlyIncomes(), currency))
                .append(" en ingresos y ").append(money(summary.monthlyPaidExpenses(), currency))
                .append(" en gastos pagados. ");
        reply.append("Tu saldo actual es ").append(money(summary.currentBalance(), currency));

        if (summary.pendingObligations().compareTo(BigDecimal.ZERO) > 0) {
            reply.append(" y, descontando ").append(money(summary.pendingObligations(), currency))
                    .append(" que aún debes pagar, te quedan ")
                    .append(money(summary.availableMoney(), currency)).append(" disponibles. ");
        } else {
            reply.append(" y no tienes cuentas pendientes por pagar. ");
        }

        if (!overdue.isEmpty()) {
            reply.append("Ojo: tienes ").append(overdue.size())
                    .append(overdue.size() == 1 ? " cuenta atrasada" : " cuentas atrasadas")
                    .append(" (").append(money(totalOf(overdue), currency)).append("). ");
        }

        summary.expensesByCategory().stream().findFirst().ifPresent(slice ->
                reply.append("Donde más se te va el dinero es en ").append(slice.category())
                        .append(" con ").append(money(slice.amount(), currency)).append(". "));

        reply.append(healthAdvice(summary, currency));

        return response(reply.toString(), "REVIEW", List.of(
                "¿Qué cuentas tengo pendientes?",
                "¿En qué gasto más?",
                "Dame un consejo"
        ));
    }

    private AssistantReply balanceReply(String currency, DashboardService.DashboardSummary summary) {
        String detail = summary.pendingObligations().compareTo(BigDecimal.ZERO) > 0
                ? " Después de descontar " + money(summary.pendingObligations(), currency)
                    + " en cuentas pendientes, te quedan " + money(summary.availableMoney(), currency) + "."
                : " No tienes cuentas pendientes, así que ese monto está completo para ti.";
        return response(
                "Tu saldo actual es " + money(summary.currentBalance(), currency) + "." + detail,
                "BALANCE",
                List.of("Revisa mis finanzas", "¿Qué cuentas tengo pendientes?", "¿En qué gasto más?")
        );
    }

    private AssistantReply incomesReply(String currency, DashboardService.DashboardSummary summary) {
        if (summary.monthlyIncomes().compareTo(BigDecimal.ZERO) == 0) {
            return response(
                    "Todavía no registras ingresos este mes. Cuando los agregues en la sección Ingresos, "
                            + "los incluyo en el cálculo.",
                    "INCOMES",
                    DEFAULT_SUGGESTIONS
            );
        }
        return response(
                "Este mes registraste " + money(summary.monthlyIncomes(), currency) + " en ingresos, "
                        + "y llevas " + money(summary.monthlyPaidExpenses(), currency) + " en gastos pagados.",
                "INCOMES",
                List.of("Revisa mis finanzas", "¿Cuánto tengo disponible?", "Dame un consejo")
        );
    }

    private AssistantReply pendingReply(UUID spaceId, String currency) {
        LocalDate today = LocalDate.now();
        List<Expense> pending = expenseRepository.findAllBySpace(spaceId).stream()
                .filter(expense -> expense.getStatus() != ExpenseStatus.PAID)
                .sorted(Comparator.comparing(Expense::getDueDate))
                .toList();

        if (pending.isEmpty()) {
            return response("No tienes cuentas pendientes. ¡Vas al día!", "PENDING",
                    List.of("Revisa mis finanzas", "¿Cuánto tengo disponible?", "¿En qué gasto más?"));
        }

        String details = pending.stream()
                .limit(3)
                .map(expense -> expense.getName() + " (" + money(expense.getAmount(), currency)
                        + ", " + dueLabel(expense.getDueDate(), today) + ")")
                .reduce((first, next) -> first + "; " + next)
                .orElse("");

        String remaining = pending.size() > 3
                ? " Además tienes " + (pending.size() - 3) + " cuenta(s) más en la lista."
                : "";

        return response(
                "Tienes " + pending.size() + " cuenta(s) pendiente(s) por "
                        + money(totalOf(pending), currency) + ": " + details + "." + remaining,
                "PENDING",
                List.of("Revisa mis finanzas", "¿Cuánto tengo disponible?", "Dame un consejo")
        );
    }

    private AssistantReply adviceReply(UUID spaceId, String currency,
                                       DashboardService.DashboardSummary summary) {
        List<Expense> overdue = overdueExpenses(spaceId, LocalDate.now());
        StringBuilder reply = new StringBuilder();

        if (!overdue.isEmpty()) {
            reply.append("Lo primero sería ponerte al día con ").append(overdue.size())
                    .append(overdue.size() == 1 ? " cuenta atrasada" : " cuentas atrasadas")
                    .append(" por ").append(money(totalOf(overdue), currency)).append(". ");
        }

        summary.expensesByCategory().stream().findFirst().ifPresent(slice ->
                reply.append("Tu categoría más alta este mes es ").append(slice.category())
                        .append(" con ").append(money(slice.amount(), currency))
                        .append("; si quieres ahorrar, es el mejor lugar para empezar. "));

        reply.append(healthAdvice(summary, currency));

        return response(reply.toString(), "ADVICE", List.of(
                "Revisa mis finanzas",
                "¿Qué cuentas tengo pendientes?",
                "¿Cuánto tengo disponible?"
        ));
    }

    private AssistantReply expenseReply(UUID spaceId, String message, String currency,
                                        DashboardService.DashboardSummary summary) {
        YearMonth currentMonth = YearMonth.now();
        List<Expense> monthExpenses = expenseRepository.findAllBySpace(spaceId).stream()
                .filter(expense -> YearMonth.from(expense.getDueDate()).equals(currentMonth))
                .toList();

        String requestedCategory = monthExpenses.stream()
                .map(Expense::getCategory)
                .distinct()
                .filter(category -> message.contains(normalize(category)))
                .findFirst()
                .orElse(null);

        if (requestedCategory != null) {
            BigDecimal categoryTotal = monthExpenses.stream()
                    .filter(expense -> expense.getCategory().equalsIgnoreCase(requestedCategory))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return response(
                    "Este mes tienes " + money(categoryTotal, currency)
                            + " registrado en la categoría " + requestedCategory + ".",
                    "CATEGORY",
                    List.of("Revisa mis finanzas", "¿En qué gasto más?", "Dame un consejo")
            );
        }

        String largestCategory = summary.expensesByCategory().stream()
                .findFirst()
                .map(slice -> " La categoría con mayor monto es " + slice.category()
                        + " con " + money(slice.amount(), currency) + ".")
                .orElse("");

        return response(
                "Los gastos con vencimiento este mes suman "
                        + money(totalOf(monthExpenses), currency) + "." + largestCategory,
                "EXPENSES",
                List.of("Revisa mis finanzas", "¿Qué cuentas tengo pendientes?", "Dame un consejo")
        );
    }

    private AssistantReply unknownReply() {
        return response(
                "Puedo ayudarte con eso si me lo pides de otra forma. Prueba con \"revisa mis finanzas\", "
                        + "\"cuánto tengo disponible\", \"qué cuentas tengo pendientes\" o \"en qué gasto más\".",
                "UNKNOWN",
                DEFAULT_SUGGESTIONS
        );
    }

    private String healthAdvice(DashboardService.DashboardSummary summary, String currency) {
        if (summary.availableMoney().compareTo(BigDecimal.ZERO) < 0) {
            return "Tus compromisos superan lo que tienes disponible por "
                    + money(summary.availableMoney().abs(), currency)
                    + ", así que conviene priorizar los pagos más urgentes.";
        }
        if (summary.monthlyIncomes().compareTo(BigDecimal.ZERO) == 0) {
            return "Aún no registras ingresos este mes; agrégalos para que el cálculo sea más preciso.";
        }
        BigDecimal usage = summary.incomeUsagePercentage();
        if (usage.compareTo(new BigDecimal("80")) >= 0) {
            return "Ya usaste cerca del " + usage.intValue()
                    + "% de tus ingresos del mes, conviene frenar los gastos que no sean necesarios.";
        }
        if (usage.compareTo(new BigDecimal("50")) >= 0) {
            return "Llevas alrededor del " + usage.intValue()
                    + "% de tus ingresos usados, vas equilibrado pero con poco margen.";
        }
        return "Vas bien: solo has usado cerca del " + usage.intValue()
                + "% de tus ingresos del mes.";
    }

    private List<Expense> overdueExpenses(UUID spaceId, LocalDate today) {
        return expenseRepository.findAllBySpace(spaceId).stream()
                .filter(expense -> expense.effectiveStatus(today) == ExpenseStatus.OVERDUE)
                .sorted(Comparator.comparing(Expense::getDueDate))
                .toList();
    }

    private BigDecimal totalOf(List<Expense> expenses) {
        return expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AssistantReply response(String reply, String intent, List<String> suggestions) {
        return new AssistantReply(reply.trim(), intent, suggestions);
    }

    private String dueLabel(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            return "venció el " + dueDate.format(DATE_FORMAT);
        }
        if (dueDate.isEqual(today)) {
            return "vence hoy";
        }
        return "vence el " + dueDate.format(DATE_FORMAT);
    }

    private String money(BigDecimal amount, String currencyCode) {
        NumberFormat format = NumberFormat.getCurrencyInstance(CHILE);
        try {
            format.setCurrency(java.util.Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException ignored) {
            format.setCurrency(java.util.Currency.getInstance("CLP"));
        }
        format.setMaximumFractionDigits("CLP".equalsIgnoreCase(currencyCode) ? 0 : 2);
        return format.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private boolean startsWithAny(String text, String... terms) {
        for (String term : terms) {
            if (text.equals(term) || text.startsWith(term + " ") || text.startsWith(term + ",")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.toLowerCase(CHILE).trim(), Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }
}
