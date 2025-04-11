package com.TravelShare.dto.response;

import com.TravelShare.entity.ExpenseSplit;
import com.TravelShare.entity.TripParticipant;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseSplitResponse {
    Long id;
    Long expenseId;
    TripParticipant participantName;
    BigDecimal amount;
    BigDecimal percentage;
    Integer shares;
    List<MediaResponse> proofImages;
    ExpenseSplit.SettlementStatus settlementStatus;
    LocalDateTime settledAt;
    boolean isPayer;
}
