package com.TravelShare.mapper;

import com.TravelShare.dto.request.CurrencyCreationRequest;
import com.TravelShare.dto.request.CurrencyUpdateRequest;
import com.TravelShare.dto.response.CurrencyResponse;
import com.TravelShare.entity.Currency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {
    Currency toCurrency(CurrencyCreationRequest request);
    CurrencyResponse toCurrencyResponse(Currency currency);

    @Mapping(target = "code", ignore = true)
    void updateCurrency(@MappingTarget Currency currency, CurrencyUpdateRequest request);
}
