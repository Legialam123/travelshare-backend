package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Loại thông báo: EXPENSE_CREATED, EXPENSE_UPDATED, MEMBER_JOINED, GROUP_UPDATED, ...
    @Column(nullable = false)
    String type;

    // Nội dung thông báo
    @Column(nullable = false, length = 500)
    String content;

    // Tham chiếu đến group liên quan
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    Group group;

    // Tham chiếu đến user tạo sự kiện (nếu cần)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    User createdBy;

    // Tham chiếu đến đối tượng liên quan (expense, user, ...)
    Long referenceId;

    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
