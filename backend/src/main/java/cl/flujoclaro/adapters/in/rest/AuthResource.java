package cl.flujoclaro.adapters.in.rest;

import cl.flujoclaro.adapters.in.rest.dto.AuthRequests;
import cl.flujoclaro.application.dto.AuthResponse;
import cl.flujoclaro.application.service.AuthService;
import cl.flujoclaro.domain.model.User;
import cl.flujoclaro.domain.port.AvatarStoragePort;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Map;
import java.util.UUID;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;
    private final AvatarStoragePort avatarStorage;
    private final JsonWebToken jwt;

    public AuthResource(AuthService authService, AvatarStoragePort avatarStorage, JsonWebToken jwt) {
        this.authService = authService;
        this.avatarStorage = avatarStorage;
        this.jwt = jwt;
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid AuthRequests.RegisterRequest request) {
        AuthResponse response = authService.register(request.fullName, request.email, request.password);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid AuthRequests.LoginRequest request) {
        return Response.ok(authService.login(request.email, request.password)).build();
    }

    @POST
    @Path("/forgot-password")
    @PermitAll
    public Response forgotPassword(@Valid AuthRequests.ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email);
        return Response.ok(Map.of("message", "Si el correo existe, enviaremos instrucciones de recuperación")).build();
    }

    @POST
    @Path("/reset-password")
    @PermitAll
    public Response resetPassword(@Valid AuthRequests.ResetPasswordRequest request) {
        authService.resetPassword(request.token, request.newPassword);
        return Response.ok(Map.of("message", "Contraseña actualizada correctamente")).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    public Response me(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = authService.getUser(userId);
        return Response.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "country", user.getCountry() != null ? user.getCountry() : "",
                "currencyCode", user.getCurrencyCode(),
                "onboardingCompleted", user.isOnboardingCompleted(),
                "hasAvatar", user.getAvatarPath() != null,
                "reminderDays", user.getReminderDays()
        )).build();
    }

    @PUT
    @Path("/me/profile")
    @RolesAllowed("USER")
    public Response updateProfile(@Valid AuthRequests.UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = authService.updateProfile(
                userId,
                request.spaceId,
                request.fullName,
                request.country,
                request.currencyCode,
                request.reminderDays
        );
        return Response.ok(Map.of(
                "message", "Perfil actualizado",
                "fullName", user.getFullName(),
                "country", user.getCountry(),
                "currencyCode", user.getCurrencyCode(),
                "reminderDays", user.getReminderDays()
        )).build();
    }

    @POST
    @Path("/me/change-password")
    @RolesAllowed("USER")
    public Response changePassword(@Valid AuthRequests.ChangePasswordRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        authService.changePassword(userId, request.currentPassword, request.newPassword);
        return Response.ok(Map.of("message", "Contraseña actualizada correctamente")).build();
    }

    @POST
    @Path("/me/avatar")
    @RolesAllowed("USER")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadAvatar(@RestForm("avatar") FileUpload avatar) {
        UUID userId = UUID.fromString(jwt.getSubject());
        if (avatar == null || avatar.size() == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Selecciona una foto"))
                    .build();
        }
        String storageKey = avatarStorage.save(avatar.uploadedFile(), avatar.contentType());
        try {
            authService.updateAvatar(userId, storageKey);
            return Response.ok(Map.of("message", "Foto actualizada", "hasAvatar", true)).build();
        } catch (RuntimeException e) {
            avatarStorage.delete(storageKey);
            throw e;
        }
    }

    @GET
    @Path("/me/avatar")
    @RolesAllowed("USER")
    @Produces({"image/jpeg", "image/png", "image/webp"})
    public Response avatar() {
        UUID userId = UUID.fromString(jwt.getSubject());
        AvatarStoragePort.StoredAvatar avatar = authService.getAvatar(userId);
        return Response.ok(avatar.content())
                .type(avatar.contentType())
                .header("Cache-Control", "private, no-cache")
                .build();
    }
}
