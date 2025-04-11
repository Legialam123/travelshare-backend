package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvitationLinkResponse {
    String invitationToken;
    String invitationLink;
    String participantName;
}