package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name="expense")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String title;

    String description;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal convertedAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal originalAmount;

    @Column(precision = 15, scale = 6)
    BigDecimal exchangeRate;

    @Column
    LocalDateTime exchangeRateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_currency_code", nullable = false)
    Currency originalCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_currency_code", nullable = false)
    Currency convertedCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    GroupParticipant payer;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Media> attachments;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<ExpenseSplit> splits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SplitType splitType;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
    LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    User createdBy;

    LocalDateTime updatedAt;

    // Fields for expense finalization/locking
    @Column(nullable = false)
    @Builder.Default
    Boolean isLocked = false;

    LocalDateTime lockedAt;

    Long lockedByFinalizationId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isLocked == null) {
            isLocked = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SplitType {
        EQUAL, // Chia đều
        AMOUNT, // Chia theo số tiền cụ thể
        PERCENTAGE // Chia theo tỷ lệ phần trăm
    }
}
