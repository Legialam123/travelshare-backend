package com.TravelShare.dto.response;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripResponse {
    Long id;
    String name;
    String createdBy;
    LocalDateTime createdAt;
    Double budgetLimit;
    String defaultCurrency;
}
