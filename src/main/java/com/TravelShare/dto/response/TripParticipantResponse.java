package com.TravelShare.dto.response;

import com.TravelShare.entity.TripParticipant;
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

    TripParticipant.InvitationStatus status;

    String role;
    LocalDateTime joinedAt;
}
