package com.TravelShare.service;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.dto.request.RequestCreationRequest;
import com.TravelShare.dto.response.RequestResponse;
import com.TravelShare.entity.*;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.RequestMapper;
import com.TravelShare.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
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
    final GroupParticipantRepository participantRepository;

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

        if ("JOIN_GROUP_REQUEST".equals(req.getType())) {
            if (req.getReferenceId() != null) {
                // TRƯỜNG HỢP 1: Chọn participant có sẵn
                GroupParticipant participant = groupParticipantRepository
                        .findById(req.getReferenceId())
                        .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

                if (participant.getUser() != null) {
                    throw new AppException(ErrorCode.PARTICIPANT_ALREADY_LINKED);
                }

                participant.setUser(req.getSender());
                participant.setJoinedAt(LocalDateTime.now());
                groupParticipantRepository.save(participant);
            } else {
                // TRƯỜNG HỢP 2: Tạo participant mới
                // Parse tên từ content (định dạng: "UserName muốn tham gia nhóm với tên: ParticipantName")
                String content = req.getContent();
                String participantName = req.getSender().getFullName(); // default

                if (content != null && content.contains("với tên: ")) {
                    participantName = content.substring(content.lastIndexOf("với tên: ") + 9);
                }

                GroupParticipant newParticipant = GroupParticipant.builder()
                        .group(req.getGroup())
                        .user(req.getSender())
                        .name(participantName)
                        .role("MEMBER")
                        .joinedAt(LocalDateTime.now())
                        .build();

                groupParticipantRepository.save(newParticipant);
            }
        }

        if ("PAYMENT_CONFIRM".equals(req.getType())) {
            // Tìm PAYMENT_REQUEST gốc theo settlementId
            Optional<Request> paymentRequestOpt = requestRepository
                    .findByReferenceIdAndType(req.getReferenceId(), "PAYMENT_REQUEST");

            // Cập nhật settlement
            Settlement settlement = settlementRepository.findById(req.getReferenceId())
                    .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
            settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
            settlement.setSettledAt(LocalDateTime.now());
            settlementRepository.save(settlement);

            // Nếu có PAYMENT_REQUEST gốc thì cập nhật luôn
            if(paymentRequestOpt.isPresent()) {
                paymentRequestOpt.get().setStatus("ACCEPTED");
                requestRepository.save(paymentRequestOpt.get());
            }
        }
        return requestMapper.toRequestResponse(req);
    }

    public RequestResponse sendPaymentConfirm(Long requestId, User user) {
        // Lấy request gốc
        Request paymentRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        // Lấy thông tin Settlement
        Settlement settlement = settlementRepository.findById(paymentRequest.getReferenceId())
                .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));

        // Kiểm tra quyền và trạng thái
        if (!paymentRequest.getReceiver().getId().equals(user.getId())) {
            throw new AppException("Bạn không có quyền xác nhận thanh toán này");
        }
        if (!paymentRequest.getStatus().equals("PENDING")) {
            throw new AppException("Request không ở trạng thái chờ thanh toán");
        }

        paymentRequest.setStatus("PENDING_CONFIRM");
        requestRepository.save(paymentRequest);

        settlement.setSettlementMethod(Settlement.SettlementMethod.CASH);
        settlementRepository.save(settlement);

        GroupParticipant from = participantRepository.findById(settlement.getToParticipant().getId())
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_EXISTED));

        String content = String.format(
                "%s xác nhận đã thanh toán cho bạn trong nhóm %s, số tiền %s %s",
                from.getName(), paymentRequest.getGroup().getName(), settlement.getAmount(), settlement.getCurrency().getCode()
        );
        // 3. Tạo request PAYMENT_CONFIRM
        Request confirmRequest = new Request();
        confirmRequest.setContent(content);
        confirmRequest.setType("PAYMENT_CONFIRM");
        confirmRequest.setSender(paymentRequest.getReceiver());
        confirmRequest.setReceiver(paymentRequest.getSender());
        confirmRequest.setGroup(paymentRequest.getGroup());
        confirmRequest.setReferenceId(paymentRequest.getReferenceId());
        confirmRequest.setStatus("PENDING");

        requestRepository.save(confirmRequest);

        return requestMapper.toRequestResponse(confirmRequest);
    }

    public RequestResponse declineRequest(Long requestId, User receiver) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));
        if (!req.getReceiver().getId().equals(receiver.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        req.setStatus("DECLINED");
        requestRepository.save(req);

        if ("JOIN_GROUP_INVITE".equals(req.getType())) {
            GroupParticipant participant = groupParticipantRepository
                    .findByGroupAndUser(req.getGroup(), receiver)
                    .orElse(null);
            if (participant != null) {
                participant.setUser(null);
                groupParticipantRepository.save(participant);
            }
        }

        if ("JOIN_GROUP_REQUEST".equals(req.getType())) {
            //Đã update status request declined ở trên
        }

            if ("PAYMENT_CONFIRM".equals(req.getType())) {
            Settlement settlement = settlementRepository.findById(req.getReferenceId())
                    .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
            settlement.setStatus(Settlement.SettlementStatus.FAILED);
            settlement.setSettledAt(LocalDateTime.now());
            settlementRepository.save(settlement);
        }

        if ("PAYMENT_REQUEST".equals(req.getType())) {
            Settlement settlement = settlementRepository.findById(req.getReferenceId())
                    .orElseThrow(() -> new AppException(ErrorCode.SETTLEMENT_NOT_FOUND));
            settlement.setStatus(Settlement.SettlementStatus.FAILED);
            settlement.setSettledAt(LocalDateTime.now());
            settlementRepository.save(settlement);
        }


        return requestMapper.toRequestResponse(req);
    }
    public String buildRequestContent(Request request, User currentUser) {
        String type = request.getType();
        boolean isSender = request.getSender().getId().equals(currentUser.getId());
        String groupName = request.getGroup() != null ? request.getGroup().getName() : "";
        String receiverName = request.getReceiver().getFullName();
        String senderName = request.getSender().getFullName();
        String amount = "";

        // Nếu là request liên quan đến settlement, lấy amount từ settlement
        if ("PAYMENT_REQUEST".equals(type) || "PAYMENT_CONFIRM".equals(type)) {
            if (request.getReferenceId() != null) {
                Optional<Settlement> settlementOpt = settlementRepository.findById(request.getReferenceId());
                if (settlementOpt.isPresent()) {
                    amount = settlementOpt.get().getAmount().toString();
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
            // Thêm các loại khác...
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

    public void cancelRequest(Long requestId, User user) {
        Request req = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_EXISTED));

        if (!req.getSender().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new AppException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }

        req.setStatus("CANCELLED");
        requestRepository.save(req);
    }

    public void acceptPaymentRequestBySettlementId(Long settlementId) {
        Optional<Request> reqOpt = requestRepository.findByReferenceIdAndType(settlementId, "PAYMENT_REQUEST");
        if (reqOpt.isPresent()) {
            Request req = reqOpt.get();
            req.setStatus("ACCEPTED");
            requestRepository.save(req);
        }
    }
}