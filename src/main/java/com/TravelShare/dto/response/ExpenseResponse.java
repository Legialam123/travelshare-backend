package com.TravelShare.dto.response;

import com.TravelShare.entity.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    BigDecimal originalAmount;
    CurrencyResponse originalCurrency;

    BigDecimal convertedAmount;
    CurrencyResponse convertedCurrency;

    BigDecimal exchangeRate;
    LocalDateTime exchangeRateDate;
    boolean isMultiCurrency;
    GroupResponse group;

    GroupParticipantResponse payer;

    CategoryResponse category;

    List<MediaResponse> attachments;

    List<ExpenseSplitResponse> splits;

    Expense.SplitType splitType;
    LocalDate expenseDate;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    int participantCount;
}
