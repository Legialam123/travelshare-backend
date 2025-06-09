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
public class ExpenseCreatedEvent extends ApplicationEvent {
    final Expense expense;
    final User creator;

    public ExpenseCreatedEvent(Object source, Expense expense, User creator) {
        super(source);
        this.expense = expense;
        this.creator = creator;
    }
}
