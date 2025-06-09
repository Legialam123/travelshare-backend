package com.TravelShare.mapper;

import com.TravelShare.dto.request.GroupCreationRequest;
import com.TravelShare.dto.request.GroupUpdateRequest;
import com.TravelShare.dto.response.GroupResponse;
import com.TravelShare.dto.response.GroupSummaryResponse;
import com.TravelShare.entity.Group;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "defaultCurrency", ignore = true)
    @Mapping(target = "participants", ignore = true)
    Group toGroup(GroupCreationRequest request);

    @Mapping(target = "defaultCurrency", source = "defaultCurrency.code")
    GroupResponse toGroupResponse(Group group);

    GroupSummaryResponse toGroupSummaryResponse(Group group);

    @Mapping(target = "defaultCurrency", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateGroup(@MappingTarget Group group, GroupUpdateRequest request);

}
