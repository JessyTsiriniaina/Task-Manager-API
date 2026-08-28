package io.jessytsiriniaina.taskmanagerapi.repository;

import io.jessytsiriniaina.taskmanagerapi.entity.BlockedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedTokenRepository extends JpaRepository<BlockedToken, Long> {
    boolean existsByJti(String jti);
}