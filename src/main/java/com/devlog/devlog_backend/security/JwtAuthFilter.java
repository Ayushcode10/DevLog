package com.devlog.devlog_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// WHY extends OncePerRequestFilter: guarantees this filter runs
// EXACTLY ONCE per HTTP request — not multiple times.
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Every request that needs auth sends:
        // Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy
        final String authHeader = request.getHeader("Authorization");

        // Step 1: if there's no auth header, skip this filter entirely.
        // The SecurityConfig will then decide if the endpoint needs auth.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: strip "Bearer " prefix to get the raw token string
        final String jwt = authHeader.substring(7);

        // Step 3: extract the email (subject) from the token
        final String userEmail = jwtUtil.extractUsername(jwt);

        // Step 4: only authenticate if we got an email AND
        // the user isn't already authenticated in this request
        if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 5: load the full User from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Step 6: validate the token (email match + not expired)
            if (jwtUtil.isTokenValid(jwt, userDetails)) {

                // Step 7: create an authentication object and put it
                // in the SecurityContext so the rest of the request
                // (controllers, services) knows WHO is making this call.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials null after auth
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 8: pass control to the next filter (or the controller)
        filterChain.doFilter(request, response);
    }
}