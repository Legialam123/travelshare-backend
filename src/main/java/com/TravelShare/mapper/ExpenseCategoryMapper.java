package com.TravelShare.mapper;

import com.TravelShare.dto.request.ExpenseCategoryCreationRequest;
import com.TravelShare.dto.request.ExpenseCategoryUpdateRequest;
import com.TravelShare.dto.response.ExpenseCategoryResponse;
import com.TravelShare.entity.ExpenseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExpenseCategoryMapper {
    ExpenseCategory toCategory(ExpenseCategoryCreationRequest request);
    ExpenseCategoryResponse toCategoryResponse(ExpenseCategory expenseCategory);

    @Mapping(target = "id", ignore = true)
    void updateCategory(@MappingTarget ExpenseCategory expenseCategory, ExpenseCategoryUpdateRequest request);

}
