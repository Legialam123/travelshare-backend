package com.TravelShare.mapper;

import com.TravelShare.dto.request.ExpenseFinalizationRequest;
import com.TravelShare.dto.response.ExpenseFinalizationResponse;
import com.TravelShare.entity.ExpenseFinalization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExpenseFinalizationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "finalizedAt", ignore = true)
    @Mapping(target = "deadline", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "initiatedBy", ignore = true)
    ExpenseFinalization toExpenseFinalization(ExpenseFinalizationRequest request);

    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(target = "initiatedByName", ignore = true)
    @Mapping(target = "memberResponses", ignore = true)
    ExpenseFinalizationResponse toExpenseFinalizationResponse(ExpenseFinalization finalization);

    void updateExpenseFinalization(@MappingTarget ExpenseFinalization finalization, ExpenseFinalizationRequest request);
}
