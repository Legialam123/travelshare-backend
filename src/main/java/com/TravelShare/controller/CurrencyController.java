package com.TravelShare.controller;

import com.TravelShare.dto.request.CurrencyCreationRequest;
import com.TravelShare.dto.request.CurrencyUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.CurrencyResponse;
import com.TravelShare.service.CurrencyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/currency")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CurrencyController {
    CurrencyService   currencyService;

    @GetMapping
    ApiResponse<List<CurrencyResponse>> getAllCurrencies() {
        return ApiResponse.<List<CurrencyResponse>>builder()
                .result(currencyService.getAllCurrencies())
                .build();
    }

    @GetMapping("/{currencyCode}")
    ApiResponse<CurrencyResponse> getCurrency(@PathVariable String currencyCode) {
        return ApiResponse.<CurrencyResponse>builder()
                .result(currencyService.getCurrency(currencyCode))
                .build();
    }

    @PutMapping("/{currencyCode}")
    ApiResponse<CurrencyResponse> updateCurrency(@PathVariable String currencyCode,@RequestBody CurrencyUpdateRequest request) {
        return ApiResponse.<CurrencyResponse>builder()
                .result(currencyService.updateCurrency(currencyCode, request))
                .build();
    }

    @PostMapping
    ApiResponse<CurrencyResponse> createCurrency(@RequestBody CurrencyCreationRequest request) {
        return ApiResponse.<CurrencyResponse>builder()
                .result(currencyService.createCurrency(request))
                .build();
    }

    @DeleteMapping("/{currencyCode}")
    ApiResponse<String> deleteCurrency(@PathVariable String currencyCode) {
        currencyService.deleteCurrency(currencyCode);
        return ApiResponse.<String>builder()
                .result("Currency has been deleted")
                .build();
    }
}
