package com.TravelShare.scheduler;

import com.TravelShare.entity.UserToken;
import com.TravelShare.repository.InvalidatedTokenRepository;
import com.TravelShare.repository.RefreshTokenRepository;
import com.TravelShare.repository.UserTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TokenCleanupScheduler {

    InvalidatedTokenRepository invalidatedTokenRepository;
    RefreshTokenRepository refreshTokenRepository;
    UserTokenRepository userTokenRepository;

    @Scheduled(cron = "0 0 1 ? * MON") // Runs every Monday at 1 AM
    public void cleanupExpiredVerificationTokens() {
        LocalDateTime now = LocalDateTime.now();

        List<UserToken> expiredTokens = userTokenRepository.findAll().stream()
                .filter(token -> token.getExpiredAt().isBefore(now))
                .collect(Collectors.toList());
        invalidatedTokenRepository.deleteAllByExpiryTimeLessThan(now);
        refreshTokenRepository.deleteAllExpiredBefore(now);
        userTokenRepository.deleteAll(expiredTokens);
        log.info("Expired tokens cleaned up");
        log.info("Deleted {} expired user tokens", expiredTokens.size());
    }
}