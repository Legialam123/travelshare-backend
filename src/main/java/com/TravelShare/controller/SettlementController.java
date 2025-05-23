package com.TravelShare.controller;

import com.TravelShare.dto.request.SettlementCreationRequest;
import com.TravelShare.dto.request.SettlementUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.BalanceResponse;
import com.TravelShare.dto.response.SettlementResponse;
import com.TravelShare.entity.Group;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.GroupRepository;
import com.TravelShare.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SettlementController {

    SettlementService settlementService;
    GroupRepository groupRepository;

    @GetMapping("/group/{groupId}/balances")
    public ApiResponse<List<BalanceResponse>> getTripBalances(@PathVariable Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));

        Map<Long, BigDecimal> balances = settlementService.calculateBalances(group);

        return ApiResponse.<List<BalanceResponse>>builder()
                .result(settlementService.convertToBalanceResponse(group, balances))
                .build();
    }

    @GetMapping("/group/{groupId}/suggested")
    public ApiResponse<List<SettlementResponse>> getSuggestedSettlements(@PathVariable Long groupId, @RequestParam(name = "userOnly", required = false, defaultValue = "false") boolean userOnly,  HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        List<SettlementResponse> suggestions;

        if (userOnly) {
            // Lấy username từ token (sub)
            String username = principal.getName();

            suggestions = settlementService.suggestSettlements(groupId, username);
        } else {
            suggestions = settlementService.suggestSettlements(groupId);
        }
        return ApiResponse.<List<SettlementResponse>>builder()
                .result(suggestions)
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
            @RequestBody SettlementUpdateRequest request) {
        return ApiResponse.<SettlementResponse>builder()
                .result(settlementService.updateSettlementStatus(settlementId, request))
                .build();
    }

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<SettlementResponse>> getGroupSettlements(@PathVariable Long groupId) {
        return ApiResponse.<List<SettlementResponse>>builder()
                .result(settlementService.getGroupSettlements(groupId))
                .build();
    }
}
