package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.adapters.in.rest.dto.MovementRequests;
import cl.flujoclaro.application.service.IncomeService;
import cl.flujoclaro.domain.model.Income;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.UUID;

@Path("/api/spaces/{spaceId}/incomes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class IncomeResource {

    private final IncomeService incomeService;
    private final JsonWebToken jwt;

    public IncomeResource(IncomeService incomeService, JsonWebToken jwt) {
        this.incomeService = incomeService;
        this.jwt = jwt;
    }

    @GET
    public Response list(@PathParam("spaceId") UUID spaceId,
                         @QueryParam("search") String search,
                         @QueryParam("category") String category,
                         @QueryParam("from") LocalDate from,
                         @QueryParam("to") LocalDate to) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(incomeService.list(spaceId, userId, search, category, from, to)).build();
    }

    @POST
    public Response create(@PathParam("spaceId") UUID spaceId, @Valid MovementRequests.IncomeRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Income income = incomeService.create(spaceId, userId, toCommand(request));
        return Response.status(Response.Status.CREATED).entity(income).build();
    }

    @GET
    @Path("/{incomeId}")
    public Response get(@PathParam("spaceId") UUID spaceId, @PathParam("incomeId") UUID incomeId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(incomeService.get(spaceId, incomeId, userId)).build();
    }

    @PUT
    @Path("/{incomeId}")
    public Response update(@PathParam("spaceId") UUID spaceId,
                           @PathParam("incomeId") UUID incomeId,
                           @Valid MovementRequests.IncomeRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(incomeService.update(spaceId, incomeId, userId, toCommand(request))).build();
    }

    @DELETE
    @Path("/{incomeId}")
    public Response delete(@PathParam("spaceId") UUID spaceId, @PathParam("incomeId") UUID incomeId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        incomeService.delete(spaceId, incomeId, userId);
        return Response.noContent().build();
    }

    private IncomeService.IncomeCommand toCommand(MovementRequests.IncomeRequest request) {
        return new IncomeService.IncomeCommand(
                request.description,
                request.amount,
                request.incomeDate,
                request.category,
                request.receivedBy,
                request.incomeType,
                request.frequency,
                request.paymentMethod,
                request.notes
        );
    }
}
