package com.TravelShare.dto.request;

import com.TravelShare.entity.Settlement;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementCreationRequest {
    @NotNull
    Long tripId;

    @NotNull
    Long fromParticipantId;

    @NotNull
    Long toParticipantId;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    BigDecimal amount;

    @NotNull
    String currencyCode;

    Settlement.SettlementMethod settlementMethod;

    String description;

    Settlement.SettlementStatus status;

}
