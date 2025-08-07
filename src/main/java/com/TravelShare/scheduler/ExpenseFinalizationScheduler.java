package com.TravelShare.scheduler;

import com.TravelShare.service.ExpenseFinalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseFinalizationScheduler {

    private final ExpenseFinalizationService expenseFinalizationService;

    // Chạy mỗi giờ để kiểm tra các finalization hết hạn
    @Scheduled(cron = "0 0 * * * *")
    public void processExpiredFinalizations() {
        log.info("Starting scheduled task to process expired expense finalizations");
        try {
            expenseFinalizationService.processExpiredFinalizations();
            log.info("Completed processing expired expense finalizations");
        } catch (Exception e) {
            log.error("Error processing expired expense finalizations: {}", e.getMessage(), e);
        }
    }
}