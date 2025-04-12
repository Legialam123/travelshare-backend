package com.TravelShare.repository;

import com.TravelShare.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement,Long> {
    List<Settlement> findByTripIdAndStatus(Long tripId, Settlement.SettlementStatus status);
    List<Settlement> findByFromParticipantId(Long participantId);
    List<Settlement> findByToParticipantId(Long participantId);
    List<Settlement> findByTripId(Long tripId);
}
