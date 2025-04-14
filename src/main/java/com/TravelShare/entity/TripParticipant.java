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
@Table(name="trip_participant")
public class TripParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    User user;

    // Fields for temporary users
    String name;
    String invitationToken;
    LocalDateTime invitedAt;

    @Enumerated(EnumType.STRING)
    InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    String role; // ADMIN, MEMBER, VIEWER,....

    LocalDateTime joinedAt;

    public enum InvitationStatus {
        PENDING, ACTIVE, DECLINED
    }
}
