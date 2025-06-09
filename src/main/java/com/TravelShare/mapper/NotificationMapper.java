package com.TravelShare.mapper;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.dto.response.NotificationResponse;
import com.TravelShare.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toNotification(NotificationCreationRequest request);

    @Mapping(target = "group", source = "group")
    NotificationResponse toNotificationResponse(Notification notification);

    //void updateNotification(Notification notification, NotificationUpdateRequest request);
}
