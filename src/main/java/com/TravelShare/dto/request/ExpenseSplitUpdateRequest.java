package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

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
}