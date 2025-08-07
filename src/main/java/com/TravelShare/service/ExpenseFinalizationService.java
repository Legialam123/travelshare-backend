package com.TravelShare.service;

import com.TravelShare.dto.request.ExpenseFinalizationRequest;
import com.TravelShare.dto.response.ExpenseFinalizationResponse;
import com.TravelShare.entity.*;
import com.TravelShare.event.ExpenseFinalizationEvent;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.ExpenseFinalizationMapper;
import com.TravelShare.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExpenseFinalizationService {

    ExpenseFinalizationRepository finalizationRepository;
    ExpenseFinalizationMapper finalizationMapper;
    GroupRepository groupRepository;
    UserRepository userRepository;
    RequestRepository requestRepository;
    ExpenseRepository expenseRepository;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public ExpenseFinalizationResponse initiateFinalization(ExpenseFinalizationRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        // Tìm participant của current user trong nhóm
        GroupParticipant currentParticipant = group.getParticipants().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED)); // Không phải thành viên nhóm

        // Kiểm tra quyền admin (có thể có nhiều admin)
        if (!"ADMIN".equals(currentParticipant.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Kiểm tra không có finalization PENDING nào khác
        boolean hasPendingFinalization = finalizationRepository.existsByGroupIdAndStatus(
                request.getGroupId(), ExpenseFinalization.FinalizationStatus.PENDING);
        if (hasPendingFinalization) {
            throw new AppException(ErrorCode.FINALIZATION_ALREADY_PENDING);
        }

        // Tạo ExpenseFinalization
        ExpenseFinalization finalization = finalizationMapper.toExpenseFinalization(request);
        finalization.setGroup(group);
        finalization.setInitiatedBy(currentUser.getId());
        finalization.setStatus(ExpenseFinalization.FinalizationStatus.PENDING);
        finalization.setFinalizedAt(LocalDateTime.now());

        // Set deadline
        int deadlineDays = request.getDeadlineDays() != null ? request.getDeadlineDays() : 7;
        finalization.setDeadline(LocalDateTime.now().plusDays(deadlineDays));

        finalization = finalizationRepository.save(finalization);

        // Publish event để tạo requests cho thành viên
        eventPublisher.publishEvent(new ExpenseFinalizationEvent(
                this, finalization, ExpenseFinalizationEvent.EventType.INITIATED));

        log.info("Initiated expense finalization for group {} by user {}", group.getId(), currentUser.getId());
        return buildFinalizationResponse(finalization);
    }

    @Transactional
    public void checkAndProcessFinalization(Long finalizationId) {
        ExpenseFinalization finalization = finalizationRepository.findById(finalizationId)
                .orElseThrow(() -> new AppException(ErrorCode.FINALIZATION_NOT_FOUND));

        if (!ExpenseFinalization.FinalizationStatus.PENDING.equals(finalization.getStatus())) {
            return; // Đã được xử lý rồi
        }

        // Lấy tất cả requests liên quan đến finalization này
        List<Request> requests = requestRepository.findAllByReferenceIdAndType(finalizationId, "EXPENSE_FINALIZATION");

        // Kiểm tra có request nào bị DECLINED không
        boolean hasDeclined = requests.stream()
                .anyMatch(req -> "DECLINED".equals(req.getStatus()));

        if (hasDeclined) {
            rejectFinalization(finalizationId);
            return;
        }

        // Kiểm tra tất cả requests đã được xử lý chưa
        boolean allProcessed = requests.stream()
                .allMatch(req -> "ACCEPTED".equals(req.getStatus()) || "DECLINED".equals(req.getStatus()));

        if (allProcessed) {
            approveFinalization(finalizationId);
        }
    }

    @Transactional
    public void rejectFinalization(Long finalizationId) {
        ExpenseFinalization finalization = finalizationRepository.findById(finalizationId)
                .orElseThrow(() -> new AppException(ErrorCode.FINALIZATION_NOT_FOUND));

        finalization.setStatus(ExpenseFinalization.FinalizationStatus.REJECTED);
        finalizationRepository.save(finalization);

        // Publish event để gửi notification
        eventPublisher.publishEvent(new ExpenseFinalizationEvent(
                this, finalization, ExpenseFinalizationEvent.EventType.REJECTED));

        log.info("Rejected expense finalization {} for group {}", finalizationId, finalization.getGroup().getId());
    }

    @Transactional
    public void approveFinalization(Long finalizationId) {
        ExpenseFinalization finalization = finalizationRepository.findById(finalizationId)
                .orElseThrow(() -> new AppException(ErrorCode.FINALIZATION_NOT_FOUND));

        finalization.setStatus(ExpenseFinalization.FinalizationStatus.APPROVED);
        finalizationRepository.save(finalization);

        // Khóa tất cả expenses có createdAt <= finalizedAt
        List<Expense> expensesToLock = expenseRepository.findByGroupIdAndCreatedAtLessThanEqualAndIsLocked(
                finalization.getGroup().getId(), finalization.getFinalizedAt(), false);

        for (Expense expense : expensesToLock) {
            expense.setIsLocked(true);
            expense.setLockedAt(LocalDateTime.now());
            expense.setLockedByFinalizationId(finalizationId);
        }
        expenseRepository.saveAll(expensesToLock);

        // Publish event để gửi notification
        eventPublisher.publishEvent(new ExpenseFinalizationEvent(
                this, finalization, ExpenseFinalizationEvent.EventType.APPROVED));

        log.info("Approved expense finalization {} for group {}, locked {} expenses",
                finalizationId, finalization.getGroup().getId(), expensesToLock.size());
    }

    public List<ExpenseFinalizationResponse> getGroupFinalizations(Long groupId) {
        List<ExpenseFinalization> finalizations = finalizationRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        return finalizations.stream()
                .map(this::buildFinalizationResponse)
                .collect(Collectors.toList());
    }

    public ExpenseFinalizationResponse getFinalization(Long finalizationId) {
        ExpenseFinalization finalization = finalizationRepository.findById(finalizationId)
                .orElseThrow(() -> new AppException(ErrorCode.FINALIZATION_NOT_FOUND));
        return buildFinalizationResponse(finalization);
    }

    // Scheduled job để xử lý các finalization hết hạn
    @Transactional
    public void processExpiredFinalizations() {
        List<ExpenseFinalization> expiredFinalizations = finalizationRepository
                .findExpiredPendingFinalizations(LocalDateTime.now());

        for (ExpenseFinalization finalization : expiredFinalizations) {
            log.info("Processing expired finalization {}", finalization.getId());

            // Tự động approve nếu hết hạn và không có ai decline
            List<Request> requests = requestRepository.findAllByReferenceIdAndType(
                    finalization.getId(), "EXPENSE_FINALIZATION");

            boolean hasDeclined = requests.stream()
                    .anyMatch(req -> "DECLINED".equals(req.getStatus()));

            if (hasDeclined) {
                rejectFinalization(finalization.getId());
            } else {
                // Set status EXPIRED và approve
                finalization.setStatus(ExpenseFinalization.FinalizationStatus.EXPIRED);
                finalizationRepository.save(finalization);
                approveFinalization(finalization.getId());
            }
        }
    }

    private ExpenseFinalizationResponse buildFinalizationResponse(ExpenseFinalization finalization) {
        ExpenseFinalizationResponse response = finalizationMapper.toExpenseFinalizationResponse(finalization);

        // Set initiatedByName - lấy từ participant name thay vì user.fullName
        String initiatedByName = finalization.getGroup().getParticipants().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(finalization.getInitiatedBy()))
                .findFirst()
                .map(GroupParticipant::getName)  // Lấy tên participant
                .orElse(null);

        // Fallback to user.fullName if participant not found
        if (initiatedByName == null) {
            User initiator = userRepository.findById(finalization.getInitiatedBy()).orElse(null);
            if (initiator != null) {
                initiatedByName = initiator.getFullName();
            }
        }
        response.setInitiatedByName(initiatedByName);

        // Lấy thông tin responses của các thành viên
        List<Request> requests = requestRepository.findAllByReferenceIdAndType(
                finalization.getId(), "EXPENSE_FINALIZATION");

        List<ExpenseFinalizationResponse.FinalizationRequestInfo> memberResponses = new ArrayList<>();
        for (Request request : requests) {
            // Tìm GroupParticipant tương ứng với receiver để lấy tên participant
            String participantName = finalization.getGroup().getParticipants().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(request.getReceiver().getId()))
                    .findFirst()
                    .map(GroupParticipant::getName)  // Lấy tên participant thay vì user.fullName
                    .orElse(request.getReceiver().getFullName()); // Fallback to user.fullName if not found

            ExpenseFinalizationResponse.FinalizationRequestInfo info =
                    ExpenseFinalizationResponse.FinalizationRequestInfo.builder()
                            .requestId(request.getId())
                            .participantId(request.getReceiver().getId())
                            .participantName(participantName)  // Sử dụng tên participant
                            .requestStatus(request.getStatus())
                            .respondedAt(request.getUpdatedAt())
                            .note(request.getContent())
                            .build();
            memberResponses.add(info);
        }
        response.setMemberResponses(memberResponses);

        return response;
    }
}

