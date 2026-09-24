package com.example.ecommerce.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {

        System.out.println("===== JWT SECRET CHECK =====");
        System.out.println("SECRET LENGTH = " + secret.length());
        System.out.println("SECRET HASH = " + secret.hashCode());

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(UserDetails userDetails) {

        System.out.println("===== NEW JWT CREATED =====");
        System.out.println("JWT USER = " + userDetails.getUsername());

        String token = Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 60 * 24
                        )
                )
                .signWith(getSigningKey())
                .compact();

        System.out.println(
                "GENERATED TOKEN START = "
                        + token.substring(0, 25)
        );

        return token;
    }

    public String extractUsername(String token) {

        System.out.println("===== VERIFYING JWT =====");

        System.out.println(
                "TOKEN START = "
                        + token.substring(
                        0,
                        Math.min(token.length(), 25)
                )
        );

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);

        return username.equals(userDetails.getUsername());
    }
}
