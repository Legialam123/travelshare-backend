package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupSummaryResponse {
    Long id;
    String name;
    UserSummaryResponse createdBy;
    LocalDateTime createdAt;
    Double budgetLimit;
} 