package com.prephub.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String displayName
    ) {}

    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserSummary user
    ) {}

    public record UserSummary(
            String id, String username, String email, String displayName, String role, String avatarUrl
    ) {}
}
