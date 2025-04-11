package com.TravelShare.repository;

import com.TravelShare.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    Optional<List<ExpenseSplit>> findAllByExpenseId(Long expenseId);
    Optional<List<ExpenseSplit>> findAllByParticipantId(Long participantId);
}
