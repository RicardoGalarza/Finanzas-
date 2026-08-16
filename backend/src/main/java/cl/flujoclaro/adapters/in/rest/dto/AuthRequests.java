package cl.flujoclaro.adapters.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequests {
    public static class RegisterRequest {
        @NotBlank
        public String fullName;
        @NotBlank
        @Email
        public String email;
        @NotBlank
        @Size(min = 8)
        public String password;
    }

    public static class LoginRequest {
        @NotBlank
        @Email
        public String email;
        @NotBlank
        public String password;
    }

    public static class ForgotPasswordRequest {
        @NotBlank
        @Email
        public String email;
    }

    public static class ResetPasswordRequest {
        @NotBlank
        public String token;
        @NotBlank
        @Size(min = 8)
        public String newPassword;
    }

    public static class UpdateProfileRequest {
        @NotBlank
        public String fullName;
        @NotBlank
        public String country;
        @NotBlank
        @Size(min = 3, max = 3)
        public String currencyCode;
        @Min(0)
        @Max(30)
        public int reminderDays;
        public java.util.UUID spaceId;
    }

    public static class ChangePasswordRequest {
        @NotBlank
        public String currentPassword;
        @NotBlank
        @Size(min = 8)
        public String newPassword;
    }
}
