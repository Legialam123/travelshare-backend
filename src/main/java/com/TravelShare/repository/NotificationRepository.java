package com.TravelShare.repository;

import com.TravelShare.entity.Group;
import com.TravelShare.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    List<Notification> findByGroup(Group group);
    List<Notification> findByGroupId(Long groupId);
    List<Notification> findByCreatedById(String userId);
    List<Notification> findByType(String type);
    List<Notification> findByGroupIn(List<Group> groups);

    @Query("""
    SELECT n FROM Notification n
    WHERE n.group IN :groups
    AND (:type IS NULL OR n.type = :type)
    AND (:fromDate IS NULL OR n.createdAt >= :fromDate)
    AND (:toDate IS NULL OR n.createdAt <= :toDate)
    """)
    List<Notification> findByGroupInAndFilter(
            @Param("groups") List<Group> groups,
            @Param("type") String type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
