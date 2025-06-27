package com.TravelShare.controller;

import com.TravelShare.dto.request.ExpenseCreationRequest;
import com.TravelShare.dto.request.ExpenseUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.ExpenseResponse;
import com.TravelShare.dto.response.UserExpenseSummaryResponse;
import com.TravelShare.service.ExpenseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/expense")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExpenseController {
    ExpenseService expenseService;

    @GetMapping
    public ApiResponse<List<ExpenseResponse>> getAllExpenses() {
        return ApiResponse.<List<ExpenseResponse>>builder()
                .result(expenseService.getAllExpenses())
                .build();
    }
    
    @GetMapping("/group/{groupId}")
    public ApiResponse<List<ExpenseResponse>> getAllExpensesByGroupId(@PathVariable Long groupId) {
        return ApiResponse.<List<ExpenseResponse>>builder()
                .result(expenseService.getAllExpensesByGroupId(groupId))
                .build();
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<UserExpenseSummaryResponse> getAllExpensesByUserId(@PathVariable String userId,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) Long categoryId
    )
    {
        return ApiResponse.<UserExpenseSummaryResponse>builder()
                .result(expenseService.getUserExpenseSummary(userId, startDate, endDate, groupId, categoryId))
                .build();
    }

    @GetMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> getExpense(@PathVariable Long expenseId) {
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.getExpense(expenseId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> createExpense(@RequestBody ExpenseCreationRequest request) {
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.createExpense(request))
                .build();
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @RequestBody ExpenseUpdateRequest request) {
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.updateExpense(expenseId, request))
                .build();
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<String> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId);
        return ApiResponse.<String>builder()
                .result("Expense has been deleted !!!")
                .build();
    }
}
