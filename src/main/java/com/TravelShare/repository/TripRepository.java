package com.TravelShare.repository;

import com.TravelShare.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByIdAndCreatedById(Long tripId, String userId);
    Optional<Trip> findById(Long tripId);
    List<Trip> findByParticipants_User_Id(String userId);
    Optional<Trip> findByJoinCode(String joinCode);
    List<Trip> findAllByCreatedById(String userId);
    List<Trip> findAllByDefaultCurrencyCode(String currencyCode);
    @Query("SELECT t FROM Trip t " +
            "LEFT JOIN FETCH t.participants p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH t.tripImages " +
            "WHERE t.id = :tripId")
    Optional<Trip> findByIdWithParticipants(@Param("tripId") Long tripId);

}
