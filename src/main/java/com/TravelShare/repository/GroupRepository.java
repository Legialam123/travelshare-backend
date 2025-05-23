package com.TravelShare.repository;

import com.TravelShare.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByIdAndCreatedById(Long groupId, String userId);
    List<Group> findByParticipants_User_Id(String userId);
    Optional<Group> findByJoinCode(String joinCode);
    List<Group> findAllByCreatedById(String userId);
    List<Group> findAllByDefaultCurrencyCode(String currencyCode);
    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.participants p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH g.groupImages " +
            "WHERE g.id = :groupId")
    Optional<Group> findByIdWithParticipants(@Param("groupId") Long groupId);

} 