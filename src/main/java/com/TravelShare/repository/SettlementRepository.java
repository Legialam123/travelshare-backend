package com.TravelShare.repository;

import com.TravelShare.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement,Long> {
    List<Settlement> findByGroupIdAndStatus(Long groupId, Settlement.SettlementStatus status);
    List<Settlement> findByFromParticipantId(Long participantId);
    List<Settlement> findByToParticipantId(Long participantId);
    List<Settlement> findByGroupId(Long groupId);
}
