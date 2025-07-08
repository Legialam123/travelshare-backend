package com.TravelShare.event;

import com.TravelShare.entity.Category;
import com.TravelShare.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryExpenseCreatedEvent extends ApplicationEvent {
    final Category category;
    final User creator;

    public CategoryExpenseCreatedEvent(Object source, Category category, User creator) {
        super(source);
        this.category = category;
        this.creator = creator;
    }
}
