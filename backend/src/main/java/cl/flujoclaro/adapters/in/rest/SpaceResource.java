package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/spaces")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class SpaceResource {

    private final SpaceRepositoryPort spaceRepository;
    private final JsonWebToken jwt;

    public SpaceResource(SpaceRepositoryPort spaceRepository, JsonWebToken jwt) {
        this.spaceRepository = spaceRepository;
        this.jwt = jwt;
    }

    @GET
    public Response list() {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<Map<String, Object>> spaces = spaceRepository.findMembershipsByUser(userId).stream()
                .flatMap(membership -> spaceRepository.findById(membership.getSpaceId())
                        .map(space -> Map.<String, Object>of(
                                "id", space.getId(),
                                "name", space.getName(),
                                "type", space.getType().name(),
                                "currencyCode", space.getCurrencyCode(),
                                "role", membership.getRole().name(),
                                "initialBalance", space.getInitialBalance()
                        ))
                        .stream())
                .toList();
        return Response.ok(spaces).build();
    }
}
