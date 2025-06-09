package com.TravelShare.listener;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.event.CategoryExpenseCreatedEvent;
import com.TravelShare.event.GroupUpdatedEvent;
import com.TravelShare.service.NotificationService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class GroupEventListener {
    @Autowired
    NotificationService notificationService;

    @EventListener
    public void handleGroupUpdated(GroupUpdatedEvent event) {
        log.info("GroupUpdatedEvent received for Group id: {}", event.getGroup().getId());
        var group = event.getGroup();
        var creator = event.getUpdater();

        NotificationCreationRequest notiRequest = NotificationCreationRequest.builder()
                .type("GROUP_UPDATED")
                .content("Nhóm '" + group.getName() + "' đã được cập nhật bởi" + creator.getFullName())
                .groupId(group.getId())
                .referenceId(group.getId())
                .build();
        log.info("NotificationCreationRequest: {}", notiRequest);
        notificationService.createNotification(notiRequest, creator);
    }
}