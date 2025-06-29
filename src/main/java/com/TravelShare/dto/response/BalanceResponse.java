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
    Long groupId;
    String groupName;
    String participantUserId;
    Long participantId;
    String participantName;
    BigDecimal balance;
    String currencyCode;
}