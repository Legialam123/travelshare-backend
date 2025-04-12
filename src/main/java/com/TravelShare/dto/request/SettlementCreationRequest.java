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
    private Long tripId;

    @NotNull
    private Long fromParticipantId;

    @NotNull
    private Long toParticipantId;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private String currencyCode;

    private Settlement.SettlementMethod settlementMethod;

    private String description;
}
