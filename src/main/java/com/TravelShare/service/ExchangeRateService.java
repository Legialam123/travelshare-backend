package com.TravelShare.service;

import com.TravelShare.dto.response.ExchangeRateResponse;
import com.TravelShare.dto.response.CurrencyConversionResponse;
import com.TravelShare.repository.CurrencyRepository;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExchangeRateService {
    RestTemplate restTemplate;
    CurrencyRepository currencyRepository;

    static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class ExternalApiResponse {
        String base;
        Map<String, BigDecimal> rates;
    }

    // ✅ Trả về ExchangeRateResponse thay vì BigDecimal
    @Cacheable(value = "exchange-rates", key = "#from + '_' + #to")
    public ExchangeRateResponse getExchangeRate(String from, String to) {
        log.info("🔄 Getting exchange rate: {} -> {}", from, to);

        try {
            // Same currency
            if (from.equalsIgnoreCase(to)) {
                return ExchangeRateResponse.builder()
                        .fromCurrency(from.toUpperCase())
                        .toCurrency(to.toUpperCase())
                        .rate(BigDecimal.ONE)
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            // Validate currencies exist in database
            validateCurrency(from);
            validateCurrency(to);

            BigDecimal rate = fetchRateFromAPI(from.toUpperCase(), to.toUpperCase());

            return ExchangeRateResponse.builder()
                    .fromCurrency(from.toUpperCase())
                    .toCurrency(to.toUpperCase())
                    .rate(rate)
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to fetch exchange rate: {}", e.getMessage());
            return ExchangeRateResponse.builder()
                    .fromCurrency(from.toUpperCase())
                    .toCurrency(to.toUpperCase())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    public CurrencyConversionResponse convertAmount(BigDecimal amount, String from, String to) {
        ExchangeRateResponse rateResponse = getExchangeRate(from, to);

        if (!rateResponse.isSuccess()) {
            return CurrencyConversionResponse.builder()
                    .originalAmount(amount)
                    .fromCurrency(from.toUpperCase())
                    .toCurrency(to.toUpperCase())
                    .success(false)
                    .errorMessage(rateResponse.getErrorMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        BigDecimal convertedAmount = amount.multiply(rateResponse.getRate())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("💰 Converted: {} {} -> {} {} (rate: {})",
                amount, from, convertedAmount, to, rateResponse.getRate());

        return CurrencyConversionResponse.builder()
                .originalAmount(amount)
                .fromCurrency(from.toUpperCase())
                .convertedAmount(convertedAmount)
                .toCurrency(to.toUpperCase())
                .exchangeRate(rateResponse.getRate())
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ✅ Helper method để lấy rate dạng BigDecimal (cho internal use)
    public BigDecimal getExchangeRateValue(String from, String to) {
        ExchangeRateResponse response = getExchangeRate(from, to);
        return response.isSuccess() ? response.getRate() : BigDecimal.ONE;
    }

    private BigDecimal fetchRateFromAPI(String from, String to) {
        String url = API_URL + from;
        log.info("🌐 Calling external API: {}", url);

        ExternalApiResponse response = restTemplate.getForObject(url, ExternalApiResponse.class);

        if (response != null && response.getRates() != null) {
            BigDecimal rate = response.getRates().get(to);
            if (rate != null) {
                log.info("✅ Got rate from API: 1 {} = {} {}", from, rate, to);
                return rate.setScale(6, RoundingMode.HALF_UP);
            }
        }

        throw new RuntimeException("No rate found for " + from + " to " + to);
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be empty");
        }

        if (!currencyRepository.existsByCode(currency.toUpperCase())) {
            throw new IllegalArgumentException("Currency not supported: " + currency);
        }
    }
}