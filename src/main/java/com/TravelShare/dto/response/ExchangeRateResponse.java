package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExchangeRateResponse {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;
}
