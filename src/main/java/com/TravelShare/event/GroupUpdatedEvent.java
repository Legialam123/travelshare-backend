package com.TravelShare.event;

import com.TravelShare.entity.Group;
import com.TravelShare.entity.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class GroupUpdatedEvent extends ApplicationEvent {
    Group group;
    User updater;

    public GroupUpdatedEvent(Object source, Group group, User updater) {
        super(source);
        this.group = group;
        this.updater = updater;
    }
}
