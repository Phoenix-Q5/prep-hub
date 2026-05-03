package com.prephub.security;

import com.prephub.common.ConflictException;
import com.prephub.common.Role;
import com.prephub.config.AppProperties;
import com.prephub.user.Portfolio;
import com.prephub.user.PortfolioRepository;
import com.prephub.user.User;
import com.prephub.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;
    private final AuthenticationManager authManager;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username()))
            throw new ConflictException("Username already taken");
        if (users.existsByEmailIgnoreCase(req.email()))
            throw new ConflictException("Email already registered");

        User user = User.builder()
                .username(req.username())
                .email(req.email().toLowerCase())
                .passwordHash(encoder.encode(req.password()))
                .displayName(req.displayName() != null ? req.displayName() : req.username())
                .role(Role.USER)
                .enabled(true)
                .build();
            user = users.saveAndFlush(user);

            Portfolio portfolio = new Portfolio();
            portfolio.setUser(user);
            portfolios.save(portfolio);

        return buildAuth(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.usernameOrEmail(), req.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid credentials");
        }
        User user = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(req.usernameOrEmail(), req.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return buildAuth(user);
    }

    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest req) {
        Claims c;
        try { c = jwt.parse(req.refreshToken()); }
        catch (Exception e) { throw new BadCredentialsException("Invalid refresh token"); }

        if (!"refresh".equals(c.get("type")))
            throw new BadCredentialsException("Not a refresh token");

        UUID userId = UUID.fromString(c.getSubject());
        User user = users.findById(userId).orElseThrow(() -> new BadCredentialsException("User not found"));
        return buildAuth(user);
    }

    private AuthDtos.AuthResponse buildAuth(User user) {
        String access = jwt.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refresh = jwt.generateRefreshToken(user.getId());
        return new AuthDtos.AuthResponse(
                access, refresh, "Bearer", props.jwt().expirationMs() / 1000,
                new AuthDtos.UserSummary(
                        user.getId().toString(), user.getUsername(), user.getEmail(),
                        user.getDisplayName(), user.getRole().name(),
                        user.getAvatarKey() != null ? props.storage().publicUrl() + "/avatars/" + user.getAvatarKey() : null
                ));
    }
}
