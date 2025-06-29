package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupResponse {
    Long id;
    String name;
    UserSummaryResponse createdBy;
    LocalDateTime createdAt;
    String defaultCurrency;
    String joinCode;
    List<MediaResponse> groupImages;
    List<GroupParticipantResponse> participants;
    CategoryResponse category;
} 