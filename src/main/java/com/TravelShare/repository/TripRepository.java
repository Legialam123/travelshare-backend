package com.TravelShare.repository;

import com.TravelShare.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByIdAndCreatedById(Long tripId, String userId);
    Optional<Trip> findById(Long tripId);
    List<Trip> findAllByCreatedById(String userId);
    List<Trip> findAllByDefaultCurrencyCode(String currencyCode);
}
