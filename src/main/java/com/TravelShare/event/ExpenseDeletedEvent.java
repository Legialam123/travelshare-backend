package com.TravelShare.event;

import com.TravelShare.entity.Expense;
import com.TravelShare.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEvent;

import static lombok.AccessLevel.*;

@Getter
@Setter
@FieldDefaults(level = PRIVATE)
public class ExpenseDeletedEvent extends ApplicationEvent {
    final Expense expense;
    final User deleter;

    public ExpenseDeletedEvent(Object source, Expense expense, User deleter) {
        super(source);
        this.expense = expense;
        this.deleter = deleter;
    }
}