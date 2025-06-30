package com.TravelShare.controller;

import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.CurrencyConversionResponse;
import com.TravelShare.dto.response.ExchangeRateResponse;
import com.TravelShare.service.ExchangeRateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExchangeRateController {
    ExchangeRateService exchangeRateService;

    @GetMapping("/rate")
    public ApiResponse<ExchangeRateResponse> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {

        ExchangeRateResponse response = exchangeRateService.getExchangeRate(from, to);

        return ApiResponse.<ExchangeRateResponse>builder()
                .code(response.isSuccess() ? 1000 : 1001)
                .message(response.isSuccess() ? "Success" : response.getErrorMessage())
                .result(response)
                .build();
    }

    @GetMapping("/convert")
    public ApiResponse<CurrencyConversionResponse> convertCurrency(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {

        CurrencyConversionResponse response = exchangeRateService.convertAmount(amount, from, to);

        return ApiResponse.<CurrencyConversionResponse>builder()
                .code(response.isSuccess() ? 1000 : 1001)
                .message(response.isSuccess() ? "Success" : response.getErrorMessage())
                .result(response)
                .build();
    }
}
