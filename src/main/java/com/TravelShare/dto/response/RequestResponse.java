package com.TravelShare.dto.response;

import com.TravelShare.entity.Group;
import com.TravelShare.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestResponse {
    Long id;
    String type;
    String status;
    UserSummaryResponse sender;
    UserSummaryResponse receiver;
    GroupSummaryResponse group;
    Long referenceId;
    String content;
    String actionUrl;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
