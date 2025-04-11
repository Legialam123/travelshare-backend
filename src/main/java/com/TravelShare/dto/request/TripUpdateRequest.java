package com.TravelShare.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripUpdateRequest {
    String name;
    Double budgetLimit;
    String defaultCurrencyCode;
}
