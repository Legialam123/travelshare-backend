package com.TravelShare.dto.response;

import com.TravelShare.entity.Group;
import com.TravelShare.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    Long id; // ID của thông báo
    String type; // Loại thông báo, ví dụ: EXPENSE_CREATED, MEMBER_JOINED, ...
    String content; // Nội dung thông báo
    GroupSummaryResponse group; // ID của nhóm liên quan
    UserSummaryResponse createdBy; // ID của người tạo thông báo (nếu cần)
    Long referenceId; // ID của đối tượng liên quan (expense, user, ...), có thể null nếu không có
    String createdAt; // Thời gian tạo thông báo
}
