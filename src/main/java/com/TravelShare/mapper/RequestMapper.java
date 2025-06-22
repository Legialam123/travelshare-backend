package com.TravelShare.mapper;

import com.TravelShare.dto.request.RequestCreationRequest;
import com.TravelShare.dto.response.RequestResponse;
import com.TravelShare.entity.Request;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    Request  toRequest(RequestCreationRequest request);

    RequestResponse toRequestResponse(Request request);
}
