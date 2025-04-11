package com.TravelShare.repository;

import com.TravelShare.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
    Optional<Currency> findByName(String name);
    Optional<Currency> findByCode(String code);
    Optional<Currency> findBySymbol(String symbol);
    boolean existsByName(String name);
}
