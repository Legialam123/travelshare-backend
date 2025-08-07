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
public class ExpenseFinalization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    FinalizationStatus status;

    @Column(nullable = false)
    LocalDateTime finalizedAt; // Thời điểm tất toán (snapshot thời gian)

    @Column(nullable = false)
    LocalDateTime deadline; // Hạn deadline để phản hồi

    @Column(columnDefinition = "TEXT")
    String description; // Mô tả lý do tất toán

    @Column(nullable = false)
    String initiatedBy; // User ID của trưởng nhóm

    @Column(nullable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    public enum FinalizationStatus {
        PENDING,    // Đang chờ phản hồi
        APPROVED,   // Đã được approve và khóa expenses
        REJECTED,   // Bị từ chối
        EXPIRED     // Hết hạn deadline
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        finalizedAt = LocalDateTime.now(); // Snapshot thời điểm khởi tạo
        if (status == null) {
            status = FinalizationStatus.PENDING;
        }
        if (deadline == null) {
            deadline = LocalDateTime.now().plusDays(3); // Default 7 ngày
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}