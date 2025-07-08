package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestCreationRequest {
    String type;         // Loại request: JOIN_GROUP, INVITE_MEMBER, ...
    String receiverId;     // ID người nhận request
    Long groupId;        // ID nhóm liên quan (có thể null)
    Long referenceId;    // Tham chiếu đối tượng liên quan (expense, settlement...)
    String content;      // Nội dung mô tả
    String actionUrl;    // Link thao tác nhanh (nếu có)
}
