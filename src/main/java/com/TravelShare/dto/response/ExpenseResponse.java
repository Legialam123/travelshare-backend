package com.TravelShare.dto.response;

import com.TravelShare.entity.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseResponse {
    Long id;
    String title;
    String description;
    BigDecimal amount;
    CurrencyResponse currency;
    TripResponse trip;
    UserResponse payer;
    ExpenseCategoryResponse category;
    List<MediaResponse> attachmentIds;
    List<ExpenseSplitResponse> splitIds;
    Expense.SplitType splitType;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Summary information
    BigDecimal totalSettled;
    BigDecimal totalPending;
    int participantCount;
}
