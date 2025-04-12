package com.TravelShare.repository;

import com.TravelShare.entity.Trip;
import com.TravelShare.entity.TripParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripParticipantRepository extends JpaRepository<TripParticipant, Long> {
    List<TripParticipant> findByTripId(Long tripId);
    List<TripParticipant> findAllByTrip(Trip trip);
    List<TripParticipant> findByUserId(String userId);
    Optional<TripParticipant> findById(Long participantId);
    Optional<TripParticipant> findByTripIdAndUserId(Long tripId, String userId);
    Optional<TripParticipant> findByInvitationToken(String token);
    List<TripParticipant> findByTripIdAndStatus(Long tripId, TripParticipant.InvitationStatus status);
}
