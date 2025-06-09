package com.TravelShare.listener;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.event.CategoryExpenseCreatedEvent;
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
public class CategoryExpenseEventListener {
    @Autowired
    NotificationService notificationService;

    @EventListener
    public void handleCategoryExpenseCreated(CategoryExpenseCreatedEvent event) {
        log.info("ExpenseCreatedEvent received for expense id: {}", event.getCategory().getId());
        var category = event.getCategory();
        var group = event.getCategory().getGroup();
        var creator = event.getCreator();

        NotificationCreationRequest notiRequest = NotificationCreationRequest.builder()
                .type("CATEGORY_CREATED")
                .content("Danh mục chi phí '" + category.getName() + "' đã được thêm vào nhóm " + group.getName())
                .groupId(group.getId())
                .referenceId(category.getId())
                .build();
        log.info("NotificationCreationRequest: {}", notiRequest);
        notificationService.createNotification(notiRequest, creator);
    }
}
