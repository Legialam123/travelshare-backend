package com.TravelShare.repository;

import com.TravelShare.entity.ExpenseFinalization;
import com.TravelShare.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseFinalizationRepository extends JpaRepository<ExpenseFinalization, Long> {
    // Tìm finalization theo group ID
    List<ExpenseFinalization> findByGroupId(Long groupId);

    // Tìm finalization theo group ID và status
    List<ExpenseFinalization> findByGroupIdAndStatus(Long groupId, ExpenseFinalization.FinalizationStatus status);

    // Kiểm tra có finalization PENDING nào trong group không
    boolean existsByGroupIdAndStatus(Long groupId, ExpenseFinalization.FinalizationStatus status);

    // Tìm finalization PENDING của group
    Optional<ExpenseFinalization> findByGroupAndStatus(Group group, ExpenseFinalization.FinalizationStatus status);

    // Tìm các finalization đã hết deadline nhưng vẫn PENDING (cho scheduled job)
    @Query("SELECT f FROM ExpenseFinalization f WHERE f.status = 'PENDING' AND f.deadline < :currentTime")
    List<ExpenseFinalization> findExpiredPendingFinalizations(@Param("currentTime") LocalDateTime currentTime);

    // Tìm finalization theo initiatedBy
    List<ExpenseFinalization> findByInitiatedBy(String userId);

    // Tìm finalization gần đây nhất của group
    @Query("SELECT f FROM ExpenseFinalization f WHERE f.group.id = :groupId ORDER BY f.createdAt DESC")
    List<ExpenseFinalization> findByGroupIdOrderByCreatedAtDesc(@Param("groupId") Long groupId);
}
