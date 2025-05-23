package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name="expense_split")
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    GroupParticipant participant;

    @Column(nullable = false)
    BigDecimal amount;

    @Column(precision = 5, scale = 2)
    BigDecimal percentage;

    Integer shares;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status")
    @Builder.Default
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "is_payer")
    @Builder.Default
    private boolean payer = false;

    public enum SettlementStatus {
        PENDING, SETTLED, DISPUTED
    }

    public void markAsSettled() {
        this.settlementStatus = SettlementStatus.SETTLED;
        this.settledAt = LocalDateTime.now();
    }
}
