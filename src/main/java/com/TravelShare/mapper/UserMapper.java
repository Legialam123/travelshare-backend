package com.TravelShare.mapper;

import com.TravelShare.dto.request.UserCreationRequest;
import com.TravelShare.dto.request.UserUpdateRequest;
import com.TravelShare.dto.response.UserResponse;
import com.TravelShare.dto.response.UserSummaryResponse;
import com.TravelShare.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    UserSummaryResponse toUserSummaryResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "username", ignore = true)
    default void updateUser(@MappingTarget User user, UserUpdateRequest request) {
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
    }
}
