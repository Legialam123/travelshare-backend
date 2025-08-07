package com.TravelShare.listener;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.dto.request.RequestCreationRequest;
import com.TravelShare.entity.ExpenseFinalization;
import com.TravelShare.entity.GroupParticipant;
import com.TravelShare.entity.User;
import com.TravelShare.event.ExpenseFinalizationEvent;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.ExpenseFinalizationRepository;
import com.TravelShare.repository.ExpenseRepository;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.service.NotificationService;
import com.TravelShare.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseFinalizationEventListener {
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ExpenseFinalizationRepository finalizationRepository;
    private final ExpenseRepository expenseRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleExpenseFinalizationInitiated(ExpenseFinalizationEvent event) {
        if (event.getEventType() != ExpenseFinalizationEvent.EventType.INITIATED) {
            return;
        }

        ExpenseFinalization finalization = event.getFinalization();
        log.info("Handling expense finalization initiated event for finalization {}", finalization.getId());

        try {
            User initiator = userRepository.findById(finalization.getInitiatedBy())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Tạo requests cho tất cả thành viên trừ trưởng nhóm
            List<GroupParticipant> participants = finalization.getGroup().getParticipants().stream()
                    .filter(p -> p.getUser() != null && !p.getUser().getId().equals(initiator.getId()))
                    .toList();

            if (participants.isEmpty()) {
                // Nếu nhóm chỉ có trưởng nhóm -> tự động approve
                log.info("Group {} only has admin member, auto-approving finalization {}",
                        finalization.getGroup().getId(), finalization.getId());
                autoApproveFinalization(finalization);
            } else {
                // Tạo requests cho các thành viên
                for (GroupParticipant participant : participants) {
                    RequestCreationRequest requestCreation = RequestCreationRequest.builder()
                            .type("EXPENSE_FINALIZATION")
                            .receiverId(participant.getUser().getId())
                            .groupId(finalization.getGroup().getId())
                            .referenceId(finalization.getId())
                            .content(String.format("Xác nhận tất toán chi phí nhóm \"%s\". %s",
                                    finalization.getGroup().getName(),
                                    finalization.getDescription() != null ? finalization.getDescription() : ""))
                            .build();

                    requestService.createRequest(requestCreation, initiator);
                }

                log.info("Created {} finalization requests for group {}",
                        participants.size(), finalization.getGroup().getId());
            }

        } catch (Exception e) {
            log.error("Error handling expense finalization initiated event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleExpenseFinalizationApproved(ExpenseFinalizationEvent event) {
        if (event.getEventType() != ExpenseFinalizationEvent.EventType.APPROVED) {
            return;
        }

        ExpenseFinalization finalization = event.getFinalization();
        log.info("Handling expense finalization approved event for finalization {}", finalization.getId());

        try {
            User initiator = userRepository.findById(finalization.getInitiatedBy())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Gửi notification thành công
            NotificationCreationRequest notification = NotificationCreationRequest.builder()
                    .type("EXPENSE_FINALIZATION_APPROVED")
                    .content(String.format("Tất toán chi phí nhóm \"%s\" đã được phê duyệt.",
                            finalization.getGroup().getName()))
                    .groupId(finalization.getGroup().getId())
                    .referenceId(finalization.getId())
                    .build();

            notificationService.createNotification(notification, initiator);

        } catch (Exception e) {
            log.error("Error handling expense finalization approved event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleExpenseFinalizationRejected(ExpenseFinalizationEvent event) {
        if (event.getEventType() != ExpenseFinalizationEvent.EventType.REJECTED) {
            return;
        }

        ExpenseFinalization finalization = event.getFinalization();
        log.info("Handling expense finalization rejected event for finalization {}", finalization.getId());

        try {
            User initiator = userRepository.findById(finalization.getInitiatedBy())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Gửi notification cho tất cả thành viên trong nhóm
            NotificationCreationRequest notification = NotificationCreationRequest.builder()
                    .type("EXPENSE_FINALIZATION_REJECTED")
                    .content(String.format("Tất toán chi phí nhóm \"%s\" đã bị từ chối",
                            finalization.getGroup().getName()))
                    .groupId(finalization.getGroup().getId())
                    .referenceId(finalization.getId())
                    .build();

            notificationService.createNotification(notification, initiator);

        } catch (Exception e) {
            log.error("Error handling expense finalization rejected event: {}", e.getMessage(), e);
        }
    }

    /**
     * Tự động approve finalization khi nhóm chỉ có trưởng nhóm
     */
    @Transactional
    private void autoApproveFinalization(ExpenseFinalization finalization) {
        try {
            // Update status to APPROVED
            finalization.setStatus(ExpenseFinalization.FinalizationStatus.APPROVED);
            finalizationRepository.save(finalization);

            // Khóa tất cả expenses có createdAt <= finalizedAt
            List<com.TravelShare.entity.Expense> expensesToLock = expenseRepository
                    .findByGroupIdAndCreatedAtLessThanEqualAndIsLocked(
                            finalization.getGroup().getId(), finalization.getFinalizedAt(), false);

            for (com.TravelShare.entity.Expense expense : expensesToLock) {
                expense.setIsLocked(true);
                expense.setLockedAt(LocalDateTime.now());
                expense.setLockedByFinalizationId(finalization.getId());
            }
            expenseRepository.saveAll(expensesToLock);

            // Publish APPROVED event để gửi notification
            eventPublisher.publishEvent(new ExpenseFinalizationEvent(
                    this, finalization, ExpenseFinalizationEvent.EventType.APPROVED));

            log.info("Auto-approved expense finalization {} for group {}, locked {} expenses",
                    finalization.getId(), finalization.getGroup().getId(), expensesToLock.size());

        } catch (Exception e) {
            log.error("Error auto-approving finalization {}: {}", finalization.getId(), e.getMessage(), e);
            throw e;
        }
    }
}

