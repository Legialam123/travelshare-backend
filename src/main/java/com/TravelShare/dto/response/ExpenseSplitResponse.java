package com.TravelShare.dto.response;

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
    GroupParticipantResponse participant;
    BigDecimal amount;
    BigDecimal percentage;
    List<MediaResponse> proofImages;
    LocalDateTime settledAt;
    boolean isPayer;
}
