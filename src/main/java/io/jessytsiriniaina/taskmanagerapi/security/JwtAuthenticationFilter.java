package io.jessytsiriniaina.taskmanagerapi.security;

import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.repository.UserRepository;
import io.jessytsiriniaina.taskmanagerapi.service.BlockedTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BlockedTokenService blockedTokenService;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, BlockedTokenService blockedTokenService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.blockedTokenService = blockedTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Long userId = jwtService.extractUserId(token);

            if (blockedTokenService.isBlocked(jwtService.extractJti(token))) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ignored) {
        }

        filterChain.doFilter(request, response);
    }
}
