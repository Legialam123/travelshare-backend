package com.TravelShare.event;

import com.TravelShare.entity.Expense;
import com.TravelShare.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseUpdatedEvent extends ApplicationEvent {
    final Expense expense;
    final User updater;

    public ExpenseUpdatedEvent(Object source, Expense expense, User updater) {
        super(source);
        this.expense = expense;
        this.updater = updater;
    }
}
