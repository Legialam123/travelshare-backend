package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupCreationRequest {
    String name;
    String createdBy;
    LocalDateTime createdAt;
    String defaultCurrency;
    Long categoryId;
    String creatorName;

    // Thêm danh sách participants dự kiến
    Set<GroupInvitationRequest> participants;
}
