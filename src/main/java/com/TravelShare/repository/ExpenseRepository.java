package com.TravelShare.repository;

import com.TravelShare.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    Optional<String> findIdByPayerId(Long payerId);
    List<Expense> findAllByGroupId(Long groupId);
    @Query("SELECT e FROM Expense e WHERE e.payer.user.id = :userId")
    List<Expense> findAllByPayerUserId(@Param("userId") String userId);
}
