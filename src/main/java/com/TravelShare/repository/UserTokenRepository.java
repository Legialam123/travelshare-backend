package com.TravelShare.repository;

import com.TravelShare.entity.User;
import com.TravelShare.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, String> {
    Optional<UserToken> findByTokenAndType(String token, UserToken.TokenType type);
    List<UserToken> findByUserAndType(User user, UserToken.TokenType type);
}