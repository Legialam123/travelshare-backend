package com.TravelShare.service;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.dto.request.RequestCreationRequest;
import com.TravelShare.dto.response.RequestResponse;
import com.TravelShare.entity.*;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.RequestMapper;
import com.TravelShare.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class RequestService {
    final RequestRepository requestRepository;
    final RequestMapper requestMapper;
    final UserRepository userRepository;
    final GroupRepository groupRepository;
    final GroupParticipantRepository groupParticipantRepository;
    final NotificationService notificationService;
    final SettlementRepository settlementRepository;
    final ExpenseFinalizationService expenseFinalizationService;


    public RequestResponse createRequest(RequestCreationRequest request, User sender) {
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        Request req = requestMapper.toRequest(request);
        req.setSender(sender);
        req.setReceiver(receiver);
        req.setGroup(group);
        req.setStatus("PENDING");

        Request saved = requestRepository.save(req);

        // --- Tạo notification cho user nhận ---
        NotificationCreationRequest notiReq = NotificationCreationRequest.builder()
                .type(request.getType())
                .content(req.getContent() != null ? req.getContent() : "Bạn vừa nhận được một yêu cầu mới")
                .groupId(group.getId())
                .referenceId(saved.getId())
                .build();
        notificationService.createNotification(notiReq, sender);

        return requestMapper.toRequestResponse(saved);
    }

    public List<RequestResponse> getRequestsBySender(User sender) {
        return requestRepository.findBySender(sender)
                .stream()
                .map(req -> {
                    RequestResponse resp = requestMapper.toRequestResponse(req);
                    resp.setContent(buildRequestContent(req, sender)); // set content động theo vai trò sender
                    return resp;
                })
                .collect(Collectors.toList());
    }

    public List<RequestResponse> getMyRequests(User receiver) {
        return requestRepository.findByReceiver(receiver)
                .stream()
                .map(req -> {
                    RequestResponse resp = requestMapper.toRequestResponse(req);
                    resp.setContent(buildRequestContent(req, receiver)); // set content động theo vai trò receiver
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // Thêm method filter cho requests nhận được
    public List<RequestResponse> getMyRequestsWithFilter(
            User currentUser,
            Long groupId,
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            String direction // thêm param direction
    ) {
        List<Group> tempGroups = groupRepository.findByParticipants_User_Id(currentUser.getId());
        if (groupId != null) {
            tempGroups = tempGroups.stream()
                    .filter(g -> g.getId().equals(groupId))
                    .collect(Collectors.toList());
        }
        final List<Group> groups = tempGroups;

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Lọc theo direction
            if ("sent".equals(direction)) {
                predicates.add(cb.equal(root.get("sender").get("id"), currentUser.getId()));
            } else if ("received".equals(direction)) {
                predicates.add(cb.equal(root.get("receiver").get("id"), currentUser.getId()));
            } else {
                // Mặc định: lấy cả sent và received liên quan đến user
                predicates.add(cb.or(
                        cb.equal(root.get("sender").get("id"), currentUser.getId()),
                        cb.equal(root.get("receiver").get("id"), currentUser.getId())
                ));
            }
            predicates.add(root.get("group").in(groups));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Request> requests = requestRepository.findAll(spec);

        return requests.stream()
                .map(req -> {
                    RequestResponse resp = requestMapper.toRequestResponse(req);
                    resp.setContent(buildRequestContent(req, currentUser));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // Thêm method filter cho requests đã gửi
    public List<RequestResponse> getRequestsBySenderWithFilter(
            User sender,
            Long groupId,
            String type,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<Group> tempGroups = groupRepository.findByParticipants_User_Id(sender.getId());
        if (groupId != null) {
            tempGroups = tempGroups.stream()
                    .filter(g -> g.getId().equals(groupId))
                    .collect(Collectors.toList());
        }
        final List<Group> groups = tempGroups;

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("sender").get("id").in(sender.getId()));
            predicates.add(root.get("group").in(groups));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Request> requests = requestRepository.findAll(spec);

        return requests.stream()
                .map(req -> {
                    RequestResponse resp = requestMapper.toRequestResponse(req);
                    resp.setContent(buildRequestContent(req, sender));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    public RequestResponse acceptRequest(Long requestId, User receiver) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        if (!req.getReceiver().getId().equals(receiver.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new AppException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
        req.setStatus("ACCEPTED");
        requestRepository.save(req);

        if ("JOIN_GROUP_INVITE".equals(req.getType())) {
            GroupParticipant participant = groupParticipantRepository
                    .findById(req.getReferenceId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));
            participant.setUser(receiver);
            participant.setJoinedAt(LocalDateTime.now());
            groupParticipantRepository.save(participant);

            // Gửi notification
            notificationService.createNotification(
                    NotificationCreationRequest.builder()
                            .type("MEMBER_JOINED")
                            .content(participant.getName() + " đã tham gia vào nhóm " + req.getGroup().getName())
                            .groupId(req.getGroup().getId())
                            .build(),
                    receiver
            );
        }

        if ("PAYMENT_CONFIRM".equals(req.getType())) {
            // Xử lý xác nhận thanh toán
            if (req.getReferenceId() != null) {
                // Luồng 1: referenceId là requestId của PAYMENT_REQUEST
                Optional<Request> pendingConfirmRequestOpt = requestRepository.findById(req.getReferenceId());
                if (pendingConfirmRequestOpt.isPresent()) {
                    Request pendingConfirmRequest = pendingConfirmRequestOpt.get();
                    pendingConfirmRequest.setStatus("ACCEPTED");
                    requestRepository.save(pendingConfirmRequest);

                    Settlement settlement = settlementRepository.findById(pendingConfirmRequest.getReferenceId())
                            .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
                    settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
                    settlement.setSettledAt(LocalDateTime.now());
                    settlementRepository.save(settlement);
                } else {
                    // Luồng 2: referenceId là settlementId
                    Optional<Settlement> settlementOpt = settlementRepository.findById(req.getReferenceId());
                    if (settlementOpt.isPresent()) {
                        Settlement settlement = settlementOpt.get();
                        settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
                        settlement.setSettledAt(LocalDateTime.now());
                        settlementRepository.save(settlement);
                    } else {
                        throw new AppException(ErrorCode.SETTLEMENT_NOT_FOUND);
                    }
                }
            }
        }

        if ("EXPENSE_FINALIZATION".equals(req.getType())) {
            // Xử lý accept yêu cầu tất toán chi phí
            expenseFinalizationService.checkAndProcessFinalization(req.getReferenceId());
        }
        return requestMapper.toRequestResponse(req);
    }

    public RequestResponse declineRequest(Long requestId, User receiver) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        if (!req.getReceiver().getId().equals(receiver.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new AppException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
        req.setStatus("DECLINED");
        requestRepository.save(req);

        if ("JOIN_GROUP_INVITE".equals(req.getType())) {
            // Xóa participant nếu từ chối lời mời
            if (req.getReferenceId() != null) {
                groupParticipantRepository.deleteById(req.getReferenceId());
            }
        }
        if ("PAYMENT_REQUEST".equals(req.getType())) {
            // Xử lý từ chối yêu cầu thanh toán
            if (req.getReferenceId() != null) {
                Settlement settlement = settlementRepository.findById(req.getReferenceId())
                        .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
                settlement.setStatus(Settlement.SettlementStatus.FAILED);
                settlementRepository.save(settlement);
            }

            // Gửi notification
            notificationService.createNotification(
                    NotificationCreationRequest.builder()
                            .type("PAYMENT_REQUEST_DECLINED")
                            .content(req.getReceiver().getFullName() + " đã từ chối yêu cầu thanh toán của " + req.getSender().getFullName() + " từ nhóm " + req.getGroup().getName())
                            .groupId(req.getGroup().getId())
                            .referenceId(req.getReferenceId())
                            .build(),
                    receiver
            );
        }
        if ("PAYMENT_CONFIRM".equals(req.getType())) {
            // Xử lý từ chối xác nhận thanh toán
            if (req.getReferenceId() != null) {
                // Luồng 1: referenceId là requestId của PAYMENT_REQUEST
                Optional<Request> pendingConfirmRequestOpt = requestRepository.findById(req.getReferenceId());
                if (pendingConfirmRequestOpt.isPresent()) {
                    Request pendingConfirmRequest = pendingConfirmRequestOpt.get();
                    pendingConfirmRequest.setStatus("DECLINED");
                    requestRepository.save(pendingConfirmRequest);

                    Settlement settlement = settlementRepository.findById(pendingConfirmRequest.getReferenceId())
                            .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
                    settlement.setStatus(Settlement.SettlementStatus.FAILED);
                    settlement.setSettledAt(LocalDateTime.now());
                    settlementRepository.save(settlement);
                } else {
                    // Luồng 2: referenceId là settlementId
                    Optional<Settlement> settlementOpt = settlementRepository.findById(req.getReferenceId());
                    if (settlementOpt.isPresent()) {
                        Settlement settlement = settlementOpt.get();
                        settlement.setStatus(Settlement.SettlementStatus.FAILED);
                        settlement.setSettledAt(LocalDateTime.now());
                        settlementRepository.save(settlement);
                    } else {
                        throw new AppException(ErrorCode.SETTLEMENT_NOT_FOUND);
                    }
                }
            }
        }

        if ("EXPENSE_FINALIZATION".equals(req.getType())) {
            // Xử lý decline yêu cầu tất toán chi phí - tự động reject finalization
            expenseFinalizationService.rejectFinalization(req.getReferenceId());
        }

        return requestMapper.toRequestResponse(req);
    }

    public void cancelRequest(Long requestId, User sender) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        if (!req.getSender().getId().equals(sender.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new AppException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
        req.setStatus("CANCELLED");
        requestRepository.save(req);

        if ("PAYMENT_REQUEST".equals(req.getType())) {
            // Xử lý từ chối yêu cầu thanh toán
            if (req.getReferenceId() != null) {
                Settlement settlement = settlementRepository.findById(req.getReferenceId())
                        .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
                settlement.setStatus(Settlement.SettlementStatus.FAILED);
                settlementRepository.save(settlement);
            }

            // Gửi notification
            notificationService.createNotification(
                    NotificationCreationRequest.builder()
                            .type("PAYMENT_REQUEST_CANCELLED")
                            .content(req.getSender().getFullName() + " đã hủy bỏ yêu cầu thanh toán đến " + req.getReceiver().getFullName() + " từ nhóm " + req.getGroup().getName())
                            .groupId(req.getGroup().getId())
                            .referenceId(req.getReferenceId())
                            .build(),
                    sender
            );
        }
    }

    public void sendPaymentConfirm(Long requestId, User user) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        if (!req.getReceiver().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!"PAYMENT_REQUEST".equals(req.getType())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        req.setStatus("PENDING_CONFIRM");
        requestRepository.save(req);

        // Tạo request PAYMENT_CONFIRM mới
        Request confirmRequest = Request.builder()
                .type("PAYMENT_CONFIRM")
                .status("PENDING")
                .sender(user)
                .receiver(req.getSender())
                .group(req.getGroup())
                .referenceId(req.getId()) // Tham chiếu đến pending_confirm
                .content("Xác nhận thanh toán cho yêu cầu #" + req.getId())
                .build();

        requestRepository.save(confirmRequest);
    }

    private String buildRequestContent(Request request, User currentUser) {
        boolean isSender = request.getSender().getId().equals(currentUser.getId());
        String senderName = request.getSender().getFullName();
        String receiverName = request.getReceiver().getFullName();
        String groupName = request.getGroup() != null ? request.getGroup().getName() : "";
        String type = request.getType();
        String amount = "";

        // Lấy thông tin amount nếu có
        if ("PAYMENT_REQUEST".equals(type) || "PAYMENT_CONFIRM".equals(type)) {
            if (request.getReferenceId() != null) {
                Optional<Settlement> settlement = settlementRepository.findById(request.getReferenceId());
                if (settlement.isPresent()) {
                    amount = settlement.get().getAmount().toString();
                }
            }
        }

        switch (type) {
            case "JOIN_GROUP_INVITE":
                return isSender
                        ? "Bạn đã gửi yêu cầu mời " + receiverName + " vào nhóm " + groupName
                        : "Bạn được mời vào nhóm " + groupName;
            case "JOIN_GROUP_REQUEST":
                return isSender
                        ? "Bạn đã gửi yêu cầu tham gia nhóm " + groupName
                        : senderName + " muốn tham gia nhóm " + groupName;
            case "PAYMENT_REQUEST":
                return isSender
                        ? "Bạn đã gửi yêu cầu thanh toán " + amount + " cho " + receiverName
                        : senderName + " yêu cầu bạn thanh toán " + amount;
            case "PAYMENT_CONFIRM":
                return isSender
                        ? "Bạn đã xác nhận thanh toán " + amount + " cho " + receiverName
                        : senderName + " đã xác nhận thanh toán " + amount + " cho bạn";
            case "EXPENSE_FINALIZATION":
                return isSender
                        ? "Bạn đã gửi yêu cầu tất toán chi phí cho nhóm " + groupName
                        : "Yêu cầu xác nhận tất toán chi phí nhóm " + groupName + " từ " + senderName;
            default:
                return "Yêu cầu mới";
        }
    }

    public RequestResponse toRequestResponseWithContent(Request request, User currentUser) {
        RequestResponse response = requestMapper.toRequestResponse(request);
        response.setContent(buildRequestContent(request, currentUser));
        return response;
    }

    public void deleteRequest(Long requestId, User user) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        if (!req.getSender().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        requestRepository.delete(req);
    }

    // Method để accept payment request khi VNPay callback thành công
    public void acceptPaymentRequestBySettlementId(Long settlementId) {
        Optional<Request> reqOpt = requestRepository.findByReferenceIdAndType(settlementId, "PAYMENT_REQUEST");
        if (reqOpt.isPresent()) {
            Request req = reqOpt.get();
            req.setStatus("ACCEPTED");
            requestRepository.save(req);
        }
    }
}