package com.TravelShare.listener;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.event.ExpenseCreatedEvent;
import com.TravelShare.event.ExpenseDeletedEvent;
import com.TravelShare.event.ExpenseUpdatedEvent;
import com.TravelShare.service.NotificationService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ExpenseEventListener {
    @Autowired
    NotificationService notificationService;

    @EventListener
    public void handleExpenseCreated(ExpenseCreatedEvent event) {
        log.info("ExpenseCreatedEvent received for expense id: {}", event.getExpense().getId());
        var expense = event.getExpense();
        var group = expense.getGroup();
        var creator = event.getCreator();

        NotificationCreationRequest notiRequest = NotificationCreationRequest.builder()
                .type("EXPENSE_CREATED")
                .content("Chi phí '" + expense.getTitle() + "' đã được thêm vào nhóm " + group.getName())
                .groupId(group.getId())
                .referenceId(expense.getId())
                .build();
        log.info("NotificationCreationRequest: {}", notiRequest);
        notificationService.createNotification(notiRequest, creator);
    }

    public void handleExpenseUpdated(ExpenseUpdatedEvent event) {
        log.info("ExpenseUpdatedEvent received for expense id: {}", event.getExpense().getId());
        var expense = event.getExpense();
        var group = expense.getGroup();
        var creator = event.getUpdater();

        NotificationCreationRequest notiRequest = NotificationCreationRequest.builder()
                .type("EXPENSE_CREATED")
                .content("Chi phí '" + expense.getTitle() + "' đã được cập nhật ở nhóm " + group.getName())
                .groupId(group.getId())
                .referenceId(expense.getId())
                .build();
        log.info("NotificationCreationRequest: {}", notiRequest);
        notificationService.createNotification(notiRequest, creator);
    }

    public void handleExpenseDeleted(ExpenseDeletedEvent event) {
        log.info("ExpenseDeletedEvent received for expense id: {}", event.getExpense().getId());
        var expense = event.getExpense();
        var group = expense.getGroup();
        var creator = event.getDeleter();

        NotificationCreationRequest notiRequest = NotificationCreationRequest.builder()
                .type("EXPENSE_CREATED")
                .content("Chi phí '" + expense.getTitle() + "' đã được xóa khỏi nhóm " + group.getName())
                .groupId(group.getId())
                .referenceId(expense.getId())
                .build();
        log.info("NotificationCreationRequest: {}", notiRequest);
        notificationService.createNotification(notiRequest, creator);
    }
}
