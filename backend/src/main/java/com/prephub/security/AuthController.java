package com.prephub.security;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ResponseEntity.ok(auth.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
        return ResponseEntity.ok(auth.refresh(req));
    }
}
