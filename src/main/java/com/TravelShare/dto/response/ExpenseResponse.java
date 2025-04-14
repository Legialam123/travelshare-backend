package com.TravelShare.dto.response;

import com.TravelShare.entity.*;
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
public class ExpenseResponse {
    Long id;
    String title;

    String description;

    BigDecimal amount;

    CurrencyResponse currency;

    TripSummaryResponse trip;

    TripParticipantResponse payer;

    ExpenseCategoryResponse category;

    List<MediaResponse> attachments;

    List<ExpenseSplitResponse> splits;

    Expense.SplitType splitType;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Summary information
    BigDecimal totalSettled;
    BigDecimal totalPending;
    int participantCount;
}
