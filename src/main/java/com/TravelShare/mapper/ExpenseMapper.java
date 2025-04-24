package com.TravelShare.mapper;

import com.TravelShare.dto.request.ExpenseCreationRequest;
import com.TravelShare.dto.request.ExpenseSplitCreationRequest;
import com.TravelShare.dto.request.ExpenseUpdateRequest;
import com.TravelShare.dto.response.ExpenseResponse;
import com.TravelShare.entity.Expense;
import com.TravelShare.entity.ExpenseSplit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "trip.createdBy", ignore = true)
    @Mapping(target = "trip.defaultCurrency", ignore = true)
    @Mapping(target = "payer", ignore = true)
    @Mapping(target = "splits", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    Expense toExpense(ExpenseCreationRequest request);

    @Mapping(source = "trip.defaultCurrency.code", target = "trip.defaultCurrency")
    @Mapping(expression = "java(calculateTotalSettled(expense))", target = "totalSettled")
    @Mapping(expression = "java(calculateTotalPending(expense))", target = "totalPending")
    @Mapping(expression = "java(expense.getSplits().size())", target = "participantCount")
    ExpenseResponse toExpenseResponse(Expense expense);

    default BigDecimal calculateTotalSettled(Expense expense) {
        return expense.getSplits().stream()
                .filter(split -> split.getSettlementStatus() == ExpenseSplit.SettlementStatus.SETTLED)
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal calculateTotalPending(Expense expense) {
        return expense.getSplits().stream()
                .filter(split -> split.getSettlementStatus() == ExpenseSplit.SettlementStatus.PENDING)
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "splits", ignore = true)
    @Mapping(target = "splitType", ignore = true)
    void updateExpense(@MappingTarget Expense expense, ExpenseUpdateRequest request);
}
