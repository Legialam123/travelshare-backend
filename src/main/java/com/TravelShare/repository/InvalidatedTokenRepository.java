package com.TravelShare.repository;

import com.TravelShare.entity.InvalidatedToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM InvalidatedToken i WHERE i.expiryTime < ?1")
    void deleteAllByExpiryTimeLessThan(LocalDateTime date);
}