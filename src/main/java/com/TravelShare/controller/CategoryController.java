package com.TravelShare.controller;

import com.TravelShare.dto.request.CategoryCreationRequest;
import com.TravelShare.dto.request.CategoryUpdateRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.CategoryResponse;
import com.TravelShare.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CategoryController {
    CategoryService categoryService;

    /**
     * Lấy danh sách category dùng cho Group (hệ thống)
     */
    @GetMapping("/group")
    public ApiResponse<List<CategoryResponse>> getGroupCategories() {
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getGroupCategories())
                .build();
    }

    /**
     * Lấy danh sách category dùng cho Expense (hệ thống)
     */
    @GetMapping("/expense")
    public ApiResponse<List<CategoryResponse>> getExpenseCategories() {
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getExpenseCategories())
                .build();
    }

    /**
     * Lấy danh sách category dùng cho Expense của một group cụ thể
     */
    @GetMapping("/group/{groupId}/expense")
    public ApiResponse<List<CategoryResponse>> getGroupExpenseCategories(@PathVariable Long groupId) {
        return ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getGroupExpenseCategories(groupId))
                .build();
    }

    /**
     * Lấy thông tin chi tiết category
     */
    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.getCategory(categoryId))
                .build();
    }

    /**
     * Tạo category EXPENSE cho một group cụ thể
     */
    @PostMapping("/group/{groupId}/expense")
    public ApiResponse<CategoryResponse> createGroupExpenseCategory(
            @PathVariable Long groupId,
            @RequestBody CategoryCreationRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.createExpenseCategoryForGroup(groupId, request))
                .build();
    }

    /**
     * Tạo category hệ thống (chỉ admin mới có quyền)
     */
    @PostMapping("/system")
    public ApiResponse<CategoryResponse> createSystemCategory(@RequestBody CategoryCreationRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.createCategory(request, true))
                .build();
    }

    /**
     * Cập nhật thông tin category (chỉ được cập nhật category tự tạo)
     */
    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long categoryId, 
            @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .result(categoryService.updateExpenseCategory(categoryId, request))
                .build();
    }

    /**
     * Xóa category (chỉ được xóa category tự tạo)
     */
    @DeleteMapping("/{categoryId}")
    public ApiResponse<String> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteExpenseCategory(categoryId);
        return ApiResponse.<String>builder()
                .result("Category has been deleted")
                .build();
    }
}
