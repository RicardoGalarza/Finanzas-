package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.adapters.in.rest.dto.MovementRequests;
import cl.flujoclaro.application.service.ExpenseService;
import cl.flujoclaro.domain.exception.DomainException;
import cl.flujoclaro.domain.model.Expense;
import cl.flujoclaro.domain.port.ReceiptStoragePort;
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
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.time.LocalDate;
import java.util.UUID;

@Path("/api/spaces/{spaceId}/expenses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ExpenseResource {

    private final ExpenseService expenseService;
    private final ReceiptStoragePort receiptStorage;
    private final JsonWebToken jwt;

    public ExpenseResource(ExpenseService expenseService,
                           ReceiptStoragePort receiptStorage,
                           JsonWebToken jwt) {
        this.expenseService = expenseService;
        this.receiptStorage = receiptStorage;
        this.jwt = jwt;
    }

    @GET
    public Response list(@PathParam("spaceId") UUID spaceId,
                         @QueryParam("search") String search,
                         @QueryParam("category") String category,
                         @QueryParam("status") String status,
                         @QueryParam("from") LocalDate from,
                         @QueryParam("to") LocalDate to) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(expenseService.list(spaceId, userId, search, category, status, from, to)).build();
    }

    @POST
    public Response create(@PathParam("spaceId") UUID spaceId, @Valid MovementRequests.ExpenseRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Expense expense = expenseService.create(spaceId, userId, toCommand(request));
        return Response.status(Response.Status.CREATED).entity(expense).build();
    }

    @GET
    @Path("/{expenseId}")
    public Response get(@PathParam("spaceId") UUID spaceId, @PathParam("expenseId") UUID expenseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(expenseService.get(spaceId, expenseId, userId)).build();
    }

    @PUT
    @Path("/{expenseId}")
    public Response update(@PathParam("spaceId") UUID spaceId,
                           @PathParam("expenseId") UUID expenseId,
                           @Valid MovementRequests.ExpenseRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(expenseService.update(spaceId, expenseId, userId, toCommand(request))).build();
    }

    @POST
    @Path("/{expenseId}/pay")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response pay(@PathParam("spaceId") UUID spaceId,
                        @PathParam("expenseId") UUID expenseId,
                        @RestForm String paidAt,
                        @RestForm("receipt") FileUpload receipt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        LocalDate paymentDate;
        try {
            paymentDate = paidAt == null || paidAt.isBlank() ? LocalDate.now() : LocalDate.parse(paidAt);
        } catch (Exception e) {
            throw new DomainException("La fecha de pago no es válida");
        }

        String storageKey = null;
        try {
            if (receipt != null && receipt.size() > 0) {
                storageKey = receiptStorage.save(
                        receipt.uploadedFile(),
                        receipt.fileName(),
                        receipt.contentType()
                );
            }
            return Response.ok(
                    expenseService.markPaid(spaceId, expenseId, userId, paymentDate, storageKey)
            ).build();
        } catch (RuntimeException e) {
            if (storageKey != null) {
                receiptStorage.delete(storageKey);
            }
            throw e;
        }
    }

    @GET
    @Path("/{expenseId}/receipt")
    @Produces({"image/jpeg", "image/png", "image/webp", "application/pdf"})
    public Response receipt(@PathParam("spaceId") UUID spaceId,
                            @PathParam("expenseId") UUID expenseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ReceiptStoragePort.StoredReceipt receipt = expenseService.getReceipt(spaceId, expenseId, userId);
        return Response.ok(receipt.content())
                .type(receipt.contentType())
                .header("Content-Disposition", "inline; filename=\"" + receipt.filename() + "\"")
                .header("Cache-Control", "private, no-store")
                .build();
    }

    @DELETE
    @Path("/{expenseId}")
    public Response delete(@PathParam("spaceId") UUID spaceId, @PathParam("expenseId") UUID expenseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        expenseService.delete(spaceId, expenseId, userId);
        return Response.noContent().build();
    }

    private ExpenseService.ExpenseCommand toCommand(MovementRequests.ExpenseRequest request) {
        return new ExpenseService.ExpenseCommand(
                request.name,
                request.amount,
                request.dueDate,
                request.category,
                request.responsiblePerson,
                request.expenseType,
                request.frequency,
                request.paymentMethod,
                request.notes
        );
    }
}
