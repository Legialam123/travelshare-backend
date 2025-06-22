package com.TravelShare.dto.request;

import com.TravelShare.entity.Settlement;
import jakarta.validation.constraints.Digits;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnPaySettlementRequest {
    // Handle VnPay settlement request
    Long settlementId;

    // Handle VnPay settlement creation
    Long groupId;
    Long fromParticipantId;
    Long toParticipantId;
    @Digits(integer = 10, fraction = 2)
    BigDecimal amount;
    String currencyCode;
    Settlement.SettlementMethod settlementMethod;
    String description;
    Settlement.SettlementStatus status;
}
