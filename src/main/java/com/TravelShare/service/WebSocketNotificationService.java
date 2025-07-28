package com.TravelShare.service;

import com.TravelShare.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi thông báo đến tất cả thành viên trong group
     */
    public void sendNotificationToGroup(Long groupId, NotificationResponse notification) {
        String destination = "/topic/group/" + groupId;
        log.info("Sending notification to group {}: {}", groupId, notification);
        messagingTemplate.convertAndSend(destination, notification);
    }

    /**
     * Gửi thông báo đến user cụ thể
     */
    public void sendNotificationToUser(String username, NotificationResponse notification) {
        String destination = "/user/" + username + "/queue/notifications";
        log.info("Sending notification to user {}: {}", username, notification);
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }

    /**
     * Gửi thông báo đến nhiều user
     */
    public void sendNotificationToUsers(java.util.List<String> usernames, NotificationResponse notification) {
        usernames.forEach(username -> sendNotificationToUser(username, notification));
    }
}
