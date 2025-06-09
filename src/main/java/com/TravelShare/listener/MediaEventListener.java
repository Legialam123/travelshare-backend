package com.TravelShare.listener;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.event.GroupUpdatedEvent;
import com.TravelShare.event.MediaUploadedEvent;
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
public class MediaEventListener {
    @Autowired
    NotificationService notificationService;

    @EventListener
    public void handleMediaUploaded(MediaUploadedEvent event) {
        log.info("MediaUploadedEvent received for Group id: {}", event.getMedia().getId());
        var media = event.getMedia();
        var creator = event.getUploader();

        NotificationCreationRequest notiRequest = NotificationCreationRequest.builder()
                .type("MEDIA_UPLOADED")
                .content("Nhóm '" + media.getGroup().getName() + "' đã được cập nhật một media bởi" + creator.getFullName())
                .groupId(media.getGroup().getId())
                .referenceId(media.getId())
                .build();
        log.info("NotificationCreationRequest: {}", notiRequest);
        notificationService.createNotification(notiRequest, creator);
    }
}