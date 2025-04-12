package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BalanceResponse {
    private Long participantId;
    private String participantName;
    private BigDecimal balance;
    private String currencyCode;
}