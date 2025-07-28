package com.TravelShare.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserExpenseSummaryResponse {
    BigDecimal total;

    //Multi-currency totals
    Map<String, BigDecimal> totalsByOriginalCurrency;

    List<ExpenseResponse> expenses;
}
