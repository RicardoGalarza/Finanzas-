package cl.flujoclaro.adapters.out.security;

import cl.flujoclaro.domain.port.TokenServicePort;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtTokenService implements TokenServicePort {

    @ConfigProperty(name = "app.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "app.jwt.duration-seconds", defaultValue = "86400")
    long durationSeconds;

    @Override
    public String generateAccessToken(UUID userId, String email, String fullName) {
        return Jwt.issuer(issuer)
                .upn(email)
                .subject(userId.toString())
                .groups(Set.of("USER"))
                .claim("fullName", fullName)
                .expiresIn(Duration.ofSeconds(durationSeconds))
                .sign();
    }

    @Override
    public Map<String, Object> parseClaims(String token) {
        // Claims are resolved by Quarkus Security at request time.
        Map<String, Object> claims = new HashMap<>();
        claims.put("raw", token);
        return claims;
    }

    public UUID currentUserId(JsonWebToken jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
