package com.TravelShare.dto.request;

import com.TravelShare.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseCreationRequest {
    @NotBlank(message = "Title is required")
    String title;

    String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount;

    @NotNull(message = "Trip ID is required")
    Long tripId;

    String currency;
    @NotNull(message = "Category is required")
    Long category;

    List<Long> attachmentIds;

    Long participantId;

    @NotNull(message = "Split type is required")
    Expense.SplitType splitType = Expense.SplitType.EQUAL;

    // For non-equal splits, specify which users and their portions
    Set<ExpenseSplitCreationRequest> splits;

}
