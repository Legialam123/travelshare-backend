package com.TravelShare.service;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.dto.response.NotificationResponse;
import com.TravelShare.entity.Group;
import com.TravelShare.entity.Notification;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.NotificationMapper;
import com.TravelShare.repository.GroupRepository;
import com.TravelShare.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class NotificationService {
    final NotificationRepository notificationRepository;
    final NotificationMapper notificationMapper;
    final GroupRepository groupRepository;
    final WebSocketNotificationService webSocketNotificationService;

    public NotificationResponse createNotification(NotificationCreationRequest request, User creator) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        Notification notification = notificationMapper.toNotification(request);
        notification.setCreatedBy(creator);
        notification.setGroup(group);

        try {
            Notification saved = notificationRepository.save(notification);
            log.info("Notification saved with id: {}", saved.getId());
            NotificationResponse response = notificationMapper.toNotificationResponse(saved);

            // Gửi thông báo qua WebSocket
            webSocketNotificationService.sendNotificationToGroup(group.getId(), response);

            log.info("NotificationResponse: {}", response);
            return response;
        } catch (Exception ex) {
            log.error("Error when saving notification: ", ex);
            // Có thể throw lỗi custom hoặc trả về null, hoặc tạo một AppException mới:
            throw new AppException(ErrorCode.GROUP_NOT_EXISTED);
            // Hoặc: return null; // nếu thực sự muốn
        }
    }


    public List<NotificationResponse> getNotificationsByGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));
        List<Notification> notifications = notificationRepository.findByGroup(group);
        return notifications.stream()
                .map(notificationMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getNotificationsByUser(
            User user,
            Long groupId,
            String type,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<Group> tempGroups = groupRepository.findByParticipants_User_Id(user.getId());
        if (groupId != null) {
            tempGroups = tempGroups.stream()
                    .filter(g -> g.getId().equals(groupId))
                    .collect(Collectors.toList());
        }
        final List<Group> groups = tempGroups;

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("group").in(groups));
            predicates.add(cb.notEqual(root.get("createdBy").get("id"), user.getId()));
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

        List<Notification> notifications = notificationRepository.findAll(spec);

        return notifications.stream()
                .map(notificationMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }
}
