package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseFinalizationRequest {
    Long groupId;

    String description; // Lý do tất toán

    Integer deadlineDays; // Số ngày deadline (default 7)
}
