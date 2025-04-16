package com.TravelShare.repository;

import com.TravelShare.entity.VerificationEmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationEmailTokenRepository extends JpaRepository<VerificationEmailToken, String> {
    Optional<VerificationEmailToken> findByToken(String token);
}
