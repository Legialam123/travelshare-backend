package com.TravelShare.repository;

import com.TravelShare.entity.Group;
import com.TravelShare.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
    List<Notification> findByGroup(Group group);
    List<Notification> findByGroupId(Long groupId);
    List<Notification> findByCreatedById(String userId);
    List<Notification> findByType(String type);
    List<Notification> findByGroupIn(List<Group> groups);
}
