package com.TravelShare.controller;

import com.TravelShare.dto.request.ExpenseCreationRequest;
import com.TravelShare.dto.request.ExpenseUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.ExpenseResponse;
import com.TravelShare.service.ExpenseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        log.info("Getting all expenses");
        return ApiResponse.<List<ExpenseResponse>>builder()
                .result(expenseService.getAllExpenses())
                .build();
    }
    
    @GetMapping("/group/{groupId}")
    public ApiResponse<List<ExpenseResponse>> getAllExpensesByGroupId(@PathVariable Long groupId) {
        log.info("Getting all expenses for group with id: {}", groupId);
        return ApiResponse.<List<ExpenseResponse>>builder()
                .result(expenseService.getAllExpensesByGroupId(groupId))
                .build();
    }

    @GetMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> getExpense(@PathVariable Long expenseId) {
        log.info("Getting expense with id: {}", expenseId);
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.getExpense(expenseId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> createExpense(@RequestBody ExpenseCreationRequest request) {
        log.info("Creating new expense: {}", request.getTitle());
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.createExpense(request))
                .build();
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @RequestBody ExpenseUpdateRequest request) {
        log.info("Updating expense with id: {}", expenseId);
        return ApiResponse.<ExpenseResponse>builder()
                .result(expenseService.updateExpense(expenseId, request))
                .build();
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<String> deleteExpense(@PathVariable Long expenseId) {
        log.info("Deleting expense with id: {}", expenseId);
        expenseService.deleteExpense(expenseId);
        return ApiResponse.<String>builder()
                .result("Expense has been deleted !!!")
                .build();
    }
}
