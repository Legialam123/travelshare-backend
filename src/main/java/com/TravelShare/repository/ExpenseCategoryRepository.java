package com.TravelShare.repository;

import com.TravelShare.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    Optional<ExpenseCategory> findById(Long id);
    Optional<ExpenseCategory> findByName(String name);
    boolean existsByName(String name);
}
