package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseSplitCreationRequest {
    Long participantId;
    BigDecimal amount;
    BigDecimal percentage;
    Integer shares;
}