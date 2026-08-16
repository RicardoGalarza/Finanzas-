package cl.flujoclaro.application.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID userId,
        String email,
        String fullName,
        boolean onboardingCompleted,
        UUID defaultSpaceId
) {
}
