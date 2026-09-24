package com.example.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        System.out.println("===== JWT FILTER =====");
        System.out.println("REQUEST = "
                + request.getMethod() + " " + request.getRequestURI());
        System.out.println("AUTH HEADER PRESENT = "
                + (authHeader != null));

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            System.out.println("NO BEARER TOKEN");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.startsWith("\"") && token.endsWith("\"")) {
            token = token.substring(1, token.length() - 1);
        }

        System.out.println("TOKEN START = "
                + token.substring(0, Math.min(token.length(), 25)));

        System.out.println("TOKEN RECEIVED = YES");

        String email;

        try {

            email = jwtService.extractUsername(token);

            System.out.println("JWT EMAIL = " + email);

        } catch (Exception e) {

            System.out.println(
                    "JWT EXTRACT ERROR = "
                            + e.getClass().getName()
                            + " : "
                            + e.getMessage()
            );

            filterChain.doFilter(request, response);
            return;
        }

        if (email != null &&
                SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            try {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(email);

                System.out.println(
                        "USER FOUND = "
                                + userDetails.getUsername()
                );

                System.out.println(
                        "AUTHORITIES = "
                                + userDetails.getAuthorities()
                );

                if (jwtService.isTokenValid(
                        token,
                        userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authToken);

                    System.out.println(
                            "JWT AUTHENTICATION SUCCESS"
                    );

                } else {

                    System.out.println(
                            "JWT TOKEN INVALID"
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "USER/JWT VALIDATION ERROR = "
                                + e.getMessage()
                );

                e.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }
}