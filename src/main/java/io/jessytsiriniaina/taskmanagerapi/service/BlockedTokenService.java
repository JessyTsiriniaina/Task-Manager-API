package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.entity.BlockedToken;
import io.jessytsiriniaina.taskmanagerapi.repository.BlockedTokenRepository;
import io.jessytsiriniaina.taskmanagerapi.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockedTokenService {

    private final BlockedTokenRepository blockedTokenRepository;
    private final JwtService jwtService;

    public BlockedTokenService(BlockedTokenRepository blockedTokenRepository, JwtService jwtService) {
        this.blockedTokenRepository = blockedTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void block(String token) {
        String jti = jwtService.extractJti(token);
        if (blockedTokenRepository.existsByJti(jti)) {
            return;
        }
        blockedTokenRepository.save(new BlockedToken(jti, jwtService.extractExpiration(token)));
    }

    public boolean isBlocked(String jti) {
        return blockedTokenRepository.existsByJti(jti);
    }
}