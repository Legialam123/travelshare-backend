package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurrencyConversionResponse {
    private BigDecimal originalAmount;
    private String fromCurrency;
    private BigDecimal convertedAmount;
    private String toCurrency;
    private BigDecimal exchangeRate;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;
}
