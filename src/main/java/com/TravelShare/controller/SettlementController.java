package com.TravelShare.controller;

import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.BalanceResponse;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.Settlement;
import com.TravelShare.entity.Trip;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.TripRepository;
import com.TravelShare.service.SettlementService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SettlementController {

    SettlementService settlementService;
    TripRepository tripRepository;

    @GetMapping("/trip/{tripId}/balances")
    public ApiResponse<List<BalanceResponse>> getTripBalances(@PathVariable Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_EXISTED));

        Map<Long, BigDecimal> balances = settlementService.calculateBalances(trip);

        return ApiResponse.<List<BalanceResponse>>builder()
                .result(settlementService.convertToBalanceResponse(trip, balances))
                .build();
    }

    @GetMapping("/trip/{tripId}/suggested")
    public ApiResponse<List<SettlementResponse>> getSuggestedSettlements(@PathVariable Long tripId) {
        return ApiResponse.<List<SettlementResponse>>builder()
                .result(settlementService.suggestSettlements(tripId))
                .build();
    }

    @PostMapping
    public ApiResponse<SettlementResponse> createSettlement(
            @Valid @RequestBody SettlementCreationRequest request) {
        return ApiResponse.<SettlementResponse>builder()
                .result(settlementService.createSettlement(request))
                .build();
    }

    // ✅ 4. Xác nhận thanh toán (Confirm Settlement)
    @PatchMapping("/{settlementId}/confirm")
    public ApiResponse<SettlementResponse> updateSettlementStatus(
            @PathVariable Long settlementId,
            @RequestBody Settlement.SettlementStatus status) {
        return ApiResponse.<SettlementResponse>builder()
                .result(settlementService.updateSettlementStatus(settlementId, status))
                .build();
    }

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<SettlementResponse>> getTripSettlements(@PathVariable Long tripId) {
        return ApiResponse.<List<SettlementResponse>>builder()
                .result(settlementService.getTripSettlements(tripId))
                .build();
    }
}
