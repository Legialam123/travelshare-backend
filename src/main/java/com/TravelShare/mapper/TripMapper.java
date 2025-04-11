package com.TravelShare.mapper;

import com.TravelShare.dto.request.TripCreationRequest;
import com.TravelShare.dto.request.TripUpdateRequest;
import com.TravelShare.dto.response.TripResponse;
import com.TravelShare.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TripMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "defaultCurrency", ignore = true)
    @Mapping(target = "participants", ignore = true)
    Trip toTrip(TripCreationRequest request);

    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "defaultCurrency", source = "defaultCurrency.code")
    TripResponse toTripResponse(Trip trip);

    @Mapping(target = "defaultCurrency", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateTrip(@MappingTarget Trip trip, TripUpdateRequest request);
}
