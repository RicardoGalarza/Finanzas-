package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.application.service.CalendarService;
import cl.flujoclaro.application.service.DashboardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.UUID;

@Path("/api/spaces/{spaceId}")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class DashboardCalendarResource {

    private final DashboardService dashboardService;
    private final CalendarService calendarService;
    private final JsonWebToken jwt;

    public DashboardCalendarResource(DashboardService dashboardService,
                                     CalendarService calendarService,
                                     JsonWebToken jwt) {
        this.dashboardService = dashboardService;
        this.calendarService = calendarService;
        this.jwt = jwt;
    }

    @GET
    @Path("/dashboard")
    public Response dashboard(@PathParam("spaceId") UUID spaceId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(dashboardService.getSummary(spaceId, userId)).build();
    }

    @GET
    @Path("/calendar")
    public Response calendar(@PathParam("spaceId") UUID spaceId,
                             @QueryParam("year") int year,
                             @QueryParam("month") int month) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(calendarService.monthEvents(spaceId, userId, year, month)).build();
    }

    @GET
    @Path("/calendar/day")
    public Response day(@PathParam("spaceId") UUID spaceId, @QueryParam("date") LocalDate date) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(calendarService.dayDetail(spaceId, userId, date)).build();
    }
}
