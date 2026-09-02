package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.dto.AuthResponse;
import io.jessytsiriniaina.taskmanagerapi.dto.LoginRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.RegisterRequest;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.exception.InvalidCredentialsException;
import io.jessytsiriniaina.taskmanagerapi.exception.UserAlreadyExistsException;
import io.jessytsiriniaina.taskmanagerapi.repository.UserRepository;
import io.jessytsiriniaina.taskmanagerapi.security.JwtService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BlockedTokenService blockedTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, BlockedTokenService blockedTokenService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blockedTokenService = blockedTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenService.Grant grant = refreshTokenService.validateAndRotate(refreshToken);
        return new AuthResponse(
                jwtService.generateToken(grant.user().getId()),
                grant.refreshToken()
        );
    }

    public void logout(String token) {
        Long userId = jwtService.extractUserId(token);
        blockedTokenService.block(token);
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getId());
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .build();
    }
}
