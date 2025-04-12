package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_participant_id", nullable = false)
    TripParticipant fromParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_participant_id", nullable = false)
    TripParticipant toParticipant;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    Currency currency;

    LocalDateTime settledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SettlementStatus status;

    @Enumerated(EnumType.STRING)
    SettlementMethod settlementMethod;

    String referenceCode; // For bank transfers or e-wallets

    LocalDateTime createdAt;
    String description;

    public enum SettlementStatus {
        PENDING, COMPLETED, CANCELLED,  SUGGESTED
    }

    public enum SettlementMethod {
        CASH, BANK_TRANSFER, E_WALLET, OTHER
    }

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Media> proofImages = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if ( status == null) {
            status = SettlementStatus.PENDING;
        }
    }
}
