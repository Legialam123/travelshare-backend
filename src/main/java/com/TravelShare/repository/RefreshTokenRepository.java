package com.TravelShare.repository;

import com.TravelShare.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByIdAndRevoked(String id, boolean revoked);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.expiryTime < ?1")
    default void deleteAllExpiredBefore(LocalDateTime date) {

    }

    List<RefreshToken> findAllByUsername(String username);
}