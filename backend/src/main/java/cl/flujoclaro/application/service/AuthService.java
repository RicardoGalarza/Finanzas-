package cl.flujoclaro.application.service;

import cl.flujoclaro.application.dto.AuthResponse;
import cl.flujoclaro.domain.enums.MembershipRole;
import cl.flujoclaro.domain.enums.SpaceType;
import cl.flujoclaro.domain.exception.DomainException;
import cl.flujoclaro.domain.exception.NotFoundException;
import cl.flujoclaro.domain.exception.UnauthorizedException;
import cl.flujoclaro.domain.model.FinancialSpace;
import cl.flujoclaro.domain.model.Membership;
import cl.flujoclaro.domain.model.PasswordResetToken;
import cl.flujoclaro.domain.model.User;
import cl.flujoclaro.domain.port.AvatarStoragePort;
import cl.flujoclaro.domain.port.MailPort;
import cl.flujoclaro.domain.port.PasswordHasherPort;
import cl.flujoclaro.domain.port.PasswordResetTokenRepositoryPort;
import cl.flujoclaro.domain.port.SpaceRepositoryPort;
import cl.flujoclaro.domain.port.TokenServicePort;
import cl.flujoclaro.domain.port.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private final UserRepositoryPort userRepository;
    private final SpaceRepositoryPort spaceRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;
    private final PasswordResetTokenRepositoryPort resetTokenRepository;
    private final MailPort mailPort;
    private final AvatarStoragePort avatarStorage;
    private final String frontendUrl;

    public AuthService(UserRepositoryPort userRepository,
                       SpaceRepositoryPort spaceRepository,
                       PasswordHasherPort passwordHasher,
                       TokenServicePort tokenService,
                       PasswordResetTokenRepositoryPort resetTokenRepository,
                       MailPort mailPort,
                       AvatarStoragePort avatarStorage,
                       @ConfigProperty(name = "app.frontend-url", defaultValue = "http://localhost:5173") String frontendUrl) {
        this.userRepository = userRepository;
        this.spaceRepository = spaceRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.resetTokenRepository = resetTokenRepository;
        this.mailPort = mailPort;
        this.avatarStorage = avatarStorage;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Transactional
    public AuthResponse register(String fullName, String email, String password) {
        validatePassword(password);
        if (userRepository.existsByEmail(email)) {
            throw new DomainException("Ya existe una cuenta con ese correo");
        }
        User user = User.create(email, passwordHasher.hash(password), fullName);
        userRepository.save(user);

        FinancialSpace space = FinancialSpace.create(
                "Espacio de " + user.getFullName(),
                SpaceType.PERSONAL,
                "CLP",
                BigDecimal.ZERO,
                user.getId()
        );
        spaceRepository.save(space);
        spaceRepository.saveMembership(Membership.create(space.getId(), user.getId(), MembershipRole.ADMIN));

        return toAuthResponse(user, space.getId());
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        UUID spaceId = spaceRepository.findMembershipsByUser(user.getId()).stream()
                .findFirst()
                .map(Membership::getSpaceId)
                .orElse(null);
        return toAuthResponse(user, spaceId);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = generateRawToken();
            PasswordResetToken token = PasswordResetToken.create(
                    user.getId(),
                    hashToken(rawToken),
                    Instant.now().plus(1, ChronoUnit.HOURS)
            );
            resetTokenRepository.save(token);
            String resetUrl = frontendUrl + "/recuperar?token=" + rawToken;
            String textBody = """
                    Hola %s,

                    Recibimos una solicitud para restablecer tu contraseña en FlujoClaro.

                    Abre este enlace (válido por 1 hora):
                    %s

                    Si el enlace no funciona, copia este token en la pantalla de recuperación:
                    %s

                    Si no solicitaste este cambio, ignora este correo.
                    """.formatted(user.getFullName(), resetUrl, rawToken);
            String htmlBody = """
                    <div style="font-family:Arial,sans-serif;line-height:1.5;color:#0f172a">
                      <h2 style="margin:0 0 12px">Recuperar contraseña</h2>
                      <p>Hola <strong>%s</strong>,</p>
                      <p>Recibimos una solicitud para restablecer tu contraseña en FlujoClaro.</p>
                      <p style="margin:24px 0">
                        <a href="%s" style="background:#1e3a5f;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none;display:inline-block">
                          Restablecer contraseña
                        </a>
                      </p>
                      <p>El enlace es válido por 1 hora. Si no funciona, usa este token:</p>
                      <p style="word-break:break-all;background:#f1f5f9;padding:12px;border-radius:8px"><code>%s</code></p>
                      <p style="color:#64748b;font-size:14px">Si no solicitaste este cambio, ignora este correo.</p>
                    </div>
                    """.formatted(user.getFullName(), resetUrl, rawToken);
            mailPort.send(
                    user.getEmail(),
                    "Recuperación de contraseña - FlujoClaro",
                    textBody,
                    htmlBody
            );
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new DomainException("Token de recuperación inválido"));
        if (!token.isValid(Instant.now())) {
            throw new DomainException("El token de recuperación expiró o ya fue utilizado");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        user.updatePassword(passwordHasher.hash(newPassword));
        userRepository.save(user);
        token.markUsed();
        resetTokenRepository.save(token);
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public User updateAvatar(UUID userId, String avatarPath) {
        User user = getUser(userId);
        String previousAvatar = user.getAvatarPath();
        user.updateAvatar(avatarPath);
        User saved = userRepository.save(user);
        if (previousAvatar != null) {
            avatarStorage.delete(previousAvatar);
        }
        return saved;
    }

    public AvatarStoragePort.StoredAvatar getAvatar(UUID userId) {
        User user = getUser(userId);
        if (user.getAvatarPath() == null) {
            throw new NotFoundException("El usuario no tiene foto de perfil");
        }
        return avatarStorage.load(user.getAvatarPath());
    }

    @Transactional
    public User updateProfile(UUID userId, UUID spaceId, String fullName, String country,
                              String currencyCode, int reminderDays) {
        User user = getUser(userId);
        user.updateProfile(fullName, country, currencyCode, reminderDays);
        User saved = userRepository.save(user);

        if (spaceId != null) {
            spaceRepository.findMembership(spaceId, userId)
                    .filter(Membership::canManageMembers)
                    .ifPresent(membership -> spaceRepository.findById(spaceId).ifPresent(space -> {
                        space.setCurrencyCode(currencyCode.toUpperCase());
                        space.setUpdatedAt(Instant.now());
                        spaceRepository.save(space);
                    }));
        }
        return saved;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        User user = getUser(userId);
        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new DomainException("La contraseña actual no es correcta");
        }
        if (passwordHasher.matches(newPassword, user.getPasswordHash())) {
            throw new DomainException("La nueva contraseña debe ser diferente");
        }
        user.updatePassword(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user, UUID spaceId) {
        String token = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getFullName());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(),
                user.isOnboardingCompleted(), spaceId);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new DomainException("La contraseña debe tener al menos 8 caracteres");
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new DomainException("No se pudo procesar el token");
        }
    }
}
