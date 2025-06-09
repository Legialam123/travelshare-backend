package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationCreationRequest {
    String type; // Loại thông báo, ví dụ: EXPENSE_CREATED, MEMBER_JOINED, ...
    String content; // Nội dung thông báo
    Long groupId; // ID của nhóm liên quan
    Long referenceId; // ID của đối tượng liên quan (expense, user, ...), có thể null nếu không có
}
