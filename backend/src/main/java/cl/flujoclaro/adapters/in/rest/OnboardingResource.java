package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.adapters.in.rest.dto.OnboardingRequest;
import cl.flujoclaro.application.service.OnboardingService;
import cl.flujoclaro.domain.model.FinancialSpace;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/onboarding")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class OnboardingResource {

    private final OnboardingService onboardingService;
    private final JsonWebToken jwt;

    public OnboardingResource(OnboardingService onboardingService, JsonWebToken jwt) {
        this.onboardingService = onboardingService;
        this.jwt = jwt;
    }

    @POST
    public Response complete(@Valid OnboardingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<OnboardingService.HabitualIncome> incomes = request.incomes == null ? List.of() :
                request.incomes.stream()
                        .map(i -> new OnboardingService.HabitualIncome(
                                i.description, i.amount, i.category,
                                i.frequency != null ? i.frequency.name() : "MONTHLY"))
                        .toList();
        List<OnboardingService.HabitualBill> bills = request.bills == null ? List.of() :
                request.bills.stream()
                        .map(b -> new OnboardingService.HabitualBill(b.name, b.amount, b.category, b.dueDay))
                        .toList();

        FinancialSpace space = onboardingService.complete(userId, new OnboardingService.OnboardingCommand(
                request.fullName,
                request.country,
                request.currencyCode,
                request.initialBalance,
                request.shared,
                request.spaceName,
                incomes,
                bills
        ));
        return Response.ok(Map.of(
                "spaceId", space.getId(),
                "name", space.getName(),
                "currencyCode", space.getCurrencyCode(),
                "initialBalance", space.getInitialBalance()
        )).build();
    }
}
