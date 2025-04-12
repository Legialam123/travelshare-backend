package com.TravelShare.mapper;

import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.Settlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SettlementMapper {
    @Mapping(source = "trip.id", target = "tripId")
    @Mapping(source = "fromParticipant.id", target = "fromParticipantId")
    @Mapping(source = "fromParticipant.name", target = "fromParticipantName")
    @Mapping(source = "toParticipant.id", target = "toParticipantId")
    @Mapping(source = "toParticipant.name", target = "toParticipantName")
    @Mapping(source = "currency.code", target = "currencyCode")
    SettlementResponse toSettlementResponse(Settlement settlement);
}
