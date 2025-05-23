package com.TravelShare.dto.request;

import com.TravelShare.entity.ExpenseSplit;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseSplitUpdateRequest {
    Long participantId;
    BigDecimal amount;
    BigDecimal percentage;
    Integer shares;
    boolean payer;
    ExpenseSplit.SettlementStatus settlementStatus;
}