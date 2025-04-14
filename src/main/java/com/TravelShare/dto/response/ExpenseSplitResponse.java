package com.TravelShare.dto.response;

import com.TravelShare.entity.ExpenseSplit;
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
    TripParticipantResponse participant;
    BigDecimal amount;
    List<MediaResponse> proofImages;
    ExpenseSplit.SettlementStatus settlementStatus;
    LocalDateTime settledAt;
    boolean isPayer;
}
