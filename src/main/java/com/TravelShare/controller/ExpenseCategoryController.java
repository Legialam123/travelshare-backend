package com.TravelShare.controller;

import com.TravelShare.dto.request.ExpenseCategoryCreationRequest;
import com.TravelShare.dto.request.ExpenseCategoryUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.ExpenseCategoryResponse;
import com.TravelShare.service.ExpenseCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExpenseCategoryController {
    ExpenseCategoryService expenseCategoryService;

    @GetMapping
    ApiResponse<List<ExpenseCategoryResponse>> getALlExpenseCategory() {
        return ApiResponse.<List<ExpenseCategoryResponse>>builder()
                .result(expenseCategoryService.getAllCategories())
                .build();
    }

    @GetMapping("/{categoryId}")
    ApiResponse<ExpenseCategoryResponse> getExpenseCategory(@PathVariable Long categoryId) {
        return ApiResponse.<ExpenseCategoryResponse>builder()
                .result(expenseCategoryService.getCategory(categoryId))
                .build();
    }

    @DeleteMapping
    ApiResponse<String> deleteExpenseCategory(@PathVariable Long categoryId) {
        expenseCategoryService.deleteCategory(categoryId);
        return ApiResponse.<String>builder()
                .result("Category has been deleted")
                .build();
    }

    @PutMapping("/{categoryId}")
    ApiResponse<ExpenseCategoryResponse> updateExpenseCategory(@PathVariable Long categoryId, @RequestBody ExpenseCategoryUpdateRequest request) {
        return ApiResponse.<ExpenseCategoryResponse>builder()
                .result(expenseCategoryService.updateCategory(categoryId, request))
                .build();
    }

    @PostMapping
    ApiResponse<ExpenseCategoryResponse> createExpenseCategory(@RequestBody ExpenseCategoryCreationRequest request) {
        return ApiResponse.<ExpenseCategoryResponse>builder()
                .result(expenseCategoryService.createCategory(request))
                .build();
    }

}
