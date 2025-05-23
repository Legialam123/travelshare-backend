package com.TravelShare.repository;

import com.TravelShare.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<String> findIdByPayerId(Long payerId);
    List<Expense> findAllByGroupId(Long groupId);
}
