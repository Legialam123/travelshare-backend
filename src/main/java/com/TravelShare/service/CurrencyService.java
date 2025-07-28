package com.TravelShare.service;

import com.TravelShare.dto.request.CurrencyCreationRequest;
import com.TravelShare.dto.request.CurrencyUpdateRequest;
import com.TravelShare.dto.response.CurrencyResponse;
import com.TravelShare.entity.Currency;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.CurrencyMapper;
import com.TravelShare.repository.CurrencyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CurrencyService {
    CurrencyMapper currencyMapper;
    CurrencyRepository currencyRepository;


    public CurrencyResponse createCurrency(CurrencyCreationRequest request) {
        if (currencyRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CURRENCY_EXISTED);
        }
        Currency currency = currencyMapper.toCurrency(request);
        return currencyMapper.toCurrencyResponse(currencyRepository.save(currency));
    }

    public CurrencyResponse getCurrency(String currencyCode) {
        Currency currency = currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
        return currencyMapper.toCurrencyResponse(currency);
    }

    public CurrencyResponse updateCurrency(String currencyCode, CurrencyUpdateRequest request) {
        Currency currency = currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
        if(currencyRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CURRENCY_EXISTED);
        }
        currencyMapper.updateCurrency(currency, request);
        return currencyMapper.toCurrencyResponse(currencyRepository.save(currency));
    }

    public void deleteCurrency(String currencyCode) {
        Currency currency = currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new AppException(ErrorCode.CURRENCY_NOT_EXISTED));
        currencyRepository.delete(currency);
    }

    public List<CurrencyResponse> getAllCurrencies() {
        log.info("In method get Currencys");
        return currencyRepository.findAll().stream().map(currencyMapper::toCurrencyResponse).toList();
    }

}
