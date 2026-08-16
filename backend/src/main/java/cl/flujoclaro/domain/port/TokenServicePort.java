package cl.flujoclaro.domain.port;

import java.util.Map;
import java.util.UUID;

public interface TokenServicePort {
    String generateAccessToken(UUID userId, String email, String fullName);
    Map<String, Object> parseClaims(String token);
}
