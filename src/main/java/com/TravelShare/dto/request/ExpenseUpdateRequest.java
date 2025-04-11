package com.TravelShare.dto.request;

import com.TravelShare.entity.Expense;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseUpdateRequest {
    @NotBlank(message = "Title is required")
    String title;

    String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount;

    String currency;

    @NotNull(message = "Category is required")
    Long category;

    String payerId;

    List<Long> attachmentIds;

    @NotNull(message = "Split type is required")
    Expense.SplitType splitType;

    // For non-equal splits, specify which users and their portions
    Set<ExpenseSplitUpdateRequest> splits;
}
