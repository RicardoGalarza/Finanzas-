package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.application.service.FinancialAssistantService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/spaces/{spaceId}/assistant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class FinancialAssistantResource {

    public record AssistantRequest(
            @NotBlank(message = "Escribe una pregunta")
            @Size(max = 500, message = "La pregunta no puede superar los 500 caracteres")
            String message
    ) {}

    private final FinancialAssistantService assistantService;
    private final JsonWebToken jwt;

    public FinancialAssistantResource(FinancialAssistantService assistantService, JsonWebToken jwt) {
        this.assistantService = assistantService;
        this.jwt = jwt;
    }

    @POST
    public Response ask(@PathParam("spaceId") UUID spaceId, @Valid AssistantRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(assistantService.reply(spaceId, userId, request.message())).build();
    }
}
