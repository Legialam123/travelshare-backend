package com.TravelShare.dto.response;

import com.TravelShare.entity.TripParticipant;
import com.TravelShare.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripParticipantResponse {
    Long id;

    String name;
    UserSummaryResponse user;
    TripParticipant.InvitationStatus status;

    String role;
    LocalDateTime joinedAt;
}
