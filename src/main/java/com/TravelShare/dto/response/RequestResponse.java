package com.TravelShare.dto.response;

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
    Long senderId;
    String senderName;
    Long receiverId;
    String receiverName;
    Long groupId;
    String groupName;
    Long referenceId;
    String content;
    String actionUrl;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
