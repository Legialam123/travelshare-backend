package com.TravelShare.dto.response;

import com.TravelShare.entity.Settlement;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementResponse {
         Long id;

         Long groupId;
         String groupName;

         Long fromParticipantId;
         String fromParticipantName;


         Long toParticipantId;
         String toParticipantName;

         UserSummaryResponse fromParticipantUser;
         UserSummaryResponse toParticipantUser;

         BigDecimal amount;

         String currencyCode;

         LocalDateTime createdAt;

         LocalDateTime settledAt;

         Settlement.SettlementStatus status;

         Settlement.SettlementMethod settlementMethod;

         String description;
}

