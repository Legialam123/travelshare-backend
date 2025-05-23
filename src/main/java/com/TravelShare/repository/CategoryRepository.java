package com.TravelShare.repository;

import com.TravelShare.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByNameAndGroupIsNull(String name);
    boolean existsByName(String name);
    boolean existsByNameAndGroupId(String name, Long groupId);
    boolean existsByNameAndType(String name, Category.CategoryType type);
    boolean existsByNameAndTypeAndGroupIsNull(String name, Category.CategoryType type);
    boolean existsByNameAndTypeAndGroupId(String name, Category.CategoryType type, Long groupId);

    List<Category> findByGroupIdOrGroupIsNull(Long groupId);
    List<Category> findByType(Category.CategoryType type);
    List<Category> findByTypeIn(List<Category.CategoryType> types);
    List<Category> findByGroupId(Long groupId);
    Optional<Category> findByNameAndType(String name, Category.CategoryType type);
    
    @Query("SELECT c FROM Category c WHERE (c.type = :type1 AND c.group.id = :groupId) OR (c.type IN (:systemTypes) AND c.group IS NULL)")
    List<Category> findByTypeAndGroupIdOrTypeAndGroupIsNull(
            @Param("type1") Category.CategoryType type1, 
            @Param("groupId") Long groupId, 
            @Param("systemTypes") List<Category.CategoryType> systemTypes);

    long countByIsSystemCategory(boolean isSystemCategory);
}
