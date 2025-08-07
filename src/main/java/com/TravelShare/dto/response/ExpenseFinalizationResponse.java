package com.TravelShare.dto.response;

import com.TravelShare.entity.ExpenseFinalization;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseFinalizationResponse {
    Long id;
    Long groupId;
    String groupName;
    ExpenseFinalization.FinalizationStatus status;
    LocalDateTime finalizedAt;
    LocalDateTime deadline;
    String description;
    String initiatedBy;
    String initiatedByName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Thông tin về responses của các thành viên
    List<FinalizationRequestInfo> memberResponses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FinalizationRequestInfo {
        Long requestId;
        String participantId;
        String participantName;
        String requestStatus; // PENDING, ACCEPTED, DECLINED
        LocalDateTime respondedAt;
        String note;
    }
}
