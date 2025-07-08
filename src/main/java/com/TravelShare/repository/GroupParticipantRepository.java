package com.TravelShare.repository;

import com.TravelShare.entity.Group;
import com.TravelShare.entity.GroupParticipant;
import com.TravelShare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupParticipantRepository extends JpaRepository<GroupParticipant, Long> {
    List<GroupParticipant> findByGroupId(Long groupId);
    List<GroupParticipant> findAllByGroup(Group group);
    Optional<GroupParticipant> findByUser(User user);
    Optional<GroupParticipant> findByGroupAndUser(Group group, User user);
    Boolean existsByGroupIdAndUserId(Long groupId, String userId);
    Optional<GroupParticipant> findByName(String participantName);
    int countByGroupIdAndRole(Long groupId, String role);
    Optional<GroupParticipant> findByGroupIdAndUserId(Long groupId, String userId);
} 