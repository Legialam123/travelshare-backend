package com.TravelShare.mapper;

import com.TravelShare.dto.request.ExpenseCreationRequest;
import com.TravelShare.dto.request.ExpenseUpdateRequest;
import com.TravelShare.dto.response.ExpenseResponse;
import com.TravelShare.entity.Expense;
import com.TravelShare.entity.ExpenseSplit;
import com.TravelShare.dto.response.ExpenseSplitResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    @Mapping(target = "originalCurrency", ignore = true)
    @Mapping(target = "convertedCurrency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "group.createdBy", ignore = true)
    @Mapping(target = "group.defaultCurrency", ignore = true)
    @Mapping(target = "payer", ignore = true)
    @Mapping(target = "splits", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(source = "amount", target = "originalAmount")
    Expense toExpense(ExpenseCreationRequest request);

    @Mapping(source = "percentage", target = "percentage")
    @Mapping(source = "payer", target = "isPayer")
    ExpenseSplitResponse toExpenseSplitResponse(ExpenseSplit expenseSplit);

    @Mapping(source = "group.defaultCurrency.code", target = "group.defaultCurrency")
    @Mapping(expression = "java(expense.getSplits().size())", target = "participantCount")
    @Mapping(expression = "java(!expense.getOriginalCurrency().getCode().equals(expense.getConvertedCurrency().getCode()))", target = "isMultiCurrency")
    ExpenseResponse toExpenseResponse(Expense expense);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "originalCurrency", ignore = true)
    @Mapping(target = "convertedCurrency", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "splits", ignore = true)
    @Mapping(target = "splitType", ignore = true)
    @Mapping(source = "amount", target = "originalAmount")
    void updateExpense(@MappingTarget Expense expense, ExpenseUpdateRequest request);
}
