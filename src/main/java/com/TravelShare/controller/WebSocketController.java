package com.TravelShare.controller;

import com.TravelShare.dto.response.NotificationResponse;
import com.TravelShare.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketNotificationService webSocketNotificationService;

    @MessageMapping("/notification/ack")
    @SendTo("/topic/notification-ack")
    public String handleNotificationAck(String message) {
        log.info("Received notification acknowledgment: {}", message);
        return "Notification acknowledged: " + message;
    }

    @MessageMapping("/test-notification")
    @SendTo("/topic/test")
    public NotificationResponse handleTestNotification(NotificationResponse notification) {
        log.info("Received test notification: {}", notification);

        // Gửi lại notification đến group
        webSocketNotificationService.sendNotificationToGroup(
                notification.getGroup().getId(),
                notification
        );

        return notification;
    }
}
