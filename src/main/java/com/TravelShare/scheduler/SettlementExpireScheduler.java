package com.TravelShare.scheduler;

import com.TravelShare.entity.Settlement;
import com.TravelShare.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SettlementExpireScheduler {
    SettlementRepository settlementRepository;

        @Scheduled(fixedRate = 600_000) // Mỗi 10 phút
        @Transactional
        public void expirePendingSettlements() {
            LocalDateTime now = LocalDateTime.now();

            List<Settlement> expiredSettlements = settlementRepository
                    .findByStatusAndExpireAtBefore(Settlement.SettlementStatus.PENDING, now);

            if (!expiredSettlements.isEmpty()) {
                expiredSettlements.forEach(settlement -> {
                    settlement.setStatus(Settlement.SettlementStatus.FAILED);
                    log.debug("Settlement ID {} expired and marked as FAILED", settlement.getId());
                });

                settlementRepository.saveAll(expiredSettlements);
                log.info("Expired {} pending settlements", expiredSettlements.size());
            }
        }
    }
