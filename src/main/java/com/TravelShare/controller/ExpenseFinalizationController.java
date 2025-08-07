package com.TravelShare.controller;

import com.TravelShare.dto.request.ExpenseFinalizationRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.ExpenseFinalizationResponse;
import com.TravelShare.service.ExpenseFinalizationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense-finalization")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExpenseFinalizationController {
    ExpenseFinalizationService expenseFinalizationService;

    @PostMapping("/initiate")
    public ApiResponse<ExpenseFinalizationResponse> initiateFinalization(
            @RequestBody ExpenseFinalizationRequest request) {
        return ApiResponse.<ExpenseFinalizationResponse>builder()
                .result(expenseFinalizationService.initiateFinalization(request))
                .build();
    }

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<ExpenseFinalizationResponse>> getGroupFinalizations(
            @PathVariable Long groupId) {
        return ApiResponse.<List<ExpenseFinalizationResponse>>builder()
                .result(expenseFinalizationService.getGroupFinalizations(groupId))
                .build();
    }

    @GetMapping("/{finalizationId}")
    public ApiResponse<ExpenseFinalizationResponse> getFinalization(
            @PathVariable Long finalizationId) {
        return ApiResponse.<ExpenseFinalizationResponse>builder()
                .result(expenseFinalizationService.getFinalization(finalizationId))
                .build();
    }

    @PostMapping("/{finalizationId}/process")
    public ApiResponse<String> processFinalization(@PathVariable Long finalizationId) {
        expenseFinalizationService.checkAndProcessFinalization(finalizationId);
        return ApiResponse.<String>builder()
                .result("Finalization processed successfully")
                .build();
    }

    @PostMapping("/{finalizationId}/reject")
    public ApiResponse<String> rejectFinalization(@PathVariable Long finalizationId) {
        expenseFinalizationService.rejectFinalization(finalizationId);
        return ApiResponse.<String>builder()
                .result("Finalization rejected successfully")
                .build();
    }

    @PostMapping("/{finalizationId}/approve")
    public ApiResponse<String> approveFinalization(@PathVariable Long finalizationId) {
        expenseFinalizationService.approveFinalization(finalizationId);
        return ApiResponse.<String>builder()
                .result("Finalization approved successfully")
                .build();
    }
}

