package com.prephub.security;

import com.prephub.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties props;

    private SecretKey key() {
        String secret = props.jwt().secret();
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (Exception ignored) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String generateAccessToken(UUID userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(userId.toString())
                .claims(Map.of("username", username, "role", role, "type", "access"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.jwt().expirationMs()))
                .signWith(key())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(userId.toString())
                .claims(Map.of("type", "refresh"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.jwt().refreshExpirationMs()))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
