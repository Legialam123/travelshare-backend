package com.TravelShare.dto.request;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripCreationRequest {
    String name;
    String createdBy;
    LocalDateTime createdAt;
    Double budgetLimit;
    String defaultCurrency;

    // Thêm danh sách participants dự kiến
    Set<TripInvitationRequest> participants;
}
