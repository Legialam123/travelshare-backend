package com.TravelShare.event;

import com.TravelShare.entity.ExpenseFinalization;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExpenseFinalizationEvent extends ApplicationEvent {
    private final ExpenseFinalization finalization;
    private final EventType eventType;

    public ExpenseFinalizationEvent(Object source, ExpenseFinalization finalization, EventType eventType) {
        super(source);
        this.finalization = finalization;
        this.eventType = eventType;
    }

    public enum EventType {
        INITIATED,
        APPROVED,
        REJECTED,
        EXPIRED
    }
}
