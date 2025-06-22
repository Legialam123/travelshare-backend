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
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String type; // JOIN_GROUP, INVITE_MEMBER, PAYMENT_CONFIRMATION, ...

    @Column(nullable = false)
    String status; // PENDING, ACCEPTED, DECLINED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    User sender; // Người gửi yêu cầu

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    User receiver; // Người nhận yêu cầu

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    Group group; // Nhóm liên quan (nếu có)

    Long referenceId; // Tham chiếu đối tượng liên quan (expense, settlement...)

    String content; // Nội dung mô tả

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    String actionUrl; // link để thao tác nhanh

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
