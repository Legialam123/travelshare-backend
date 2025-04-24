package com.TravelShare.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
@Table(name="trip")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    User createdBy;

    LocalDateTime createdAt;

    @Column(unique = true, nullable = false)
    String joinCode;

    Double budgetLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_currency_code", nullable = false)
    Currency defaultCurrency;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<TripParticipant> participants = new HashSet<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<Media> tripImages = new HashSet<>();

    /*
    public void addParticipant(User user, String role, TripParticipant.InvitationStatus status) {
        TripParticipant participant = TripParticipant.builder()
                .trip(this)
                .user(user)
                .name(user.getFullName())
                .role(role)
                .status(status)
                .joinedAt(LocalDateTime.now())
                .build();

        participants.add(participant);
        user.getTrips().add(participant);
    }

    public void removeParticipant(User user) {
        participants.removeIf(participant -> participant.getUser().equals(user));
        user.getTrips().removeIf(participant -> participant.getTrip().equals(this));
    }
    */
}
