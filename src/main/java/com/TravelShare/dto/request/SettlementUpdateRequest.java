package com.TravelShare.dto.request;

import com.TravelShare.entity.Settlement;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementUpdateRequest {
    Settlement.SettlementStatus status;
}
