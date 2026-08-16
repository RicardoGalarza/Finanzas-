package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.domain.exception.DomainException;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof UnauthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
        if (exception instanceof NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
        if (exception instanceof DomainException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        }
        if (exception instanceof ConstraintViolationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Datos inválidos", "details", e.getMessage()))
                    .build();
        }
        if (exception instanceof WebApplicationException e) {
            int status = e.getResponse().getStatus();
            return Response.status(status)
                    .entity(Map.of("message", status == 404 ? "Recurso no encontrado" : "Solicitud no válida"))
                    .build();
        }
        LOG.error("Error no controlado en la API", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("message", "Error interno del servidor"))
                .build();
    }
}
