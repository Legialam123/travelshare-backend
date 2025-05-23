package com.TravelShare.dto.response;

import com.TravelShare.entity.GroupParticipant;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupParticipantResponse {
    Long id;

    String name;
    UserSummaryResponse user;
    GroupParticipant.InvitationStatus status;

    String role;
    LocalDateTime joinedAt;
}
