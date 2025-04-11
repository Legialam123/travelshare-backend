package com.TravelShare.service;

import com.TravelShare.dto.request.ExpenseCategoryCreationRequest;
import com.TravelShare.dto.request.ExpenseCategoryUpdateRequest;
import com.TravelShare.dto.response.ExpenseCategoryResponse;
import com.TravelShare.entity.ExpenseCategory;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.ExpenseCategoryMapper;
import com.TravelShare.repository.ExpenseCategoryRepository;
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
public class ExpenseCategoryService {
    ExpenseCategoryRepository expenseCategoryRepository;
    ExpenseCategoryMapper expenseCategoryMapper;

    public ExpenseCategoryResponse createCategory(ExpenseCategoryCreationRequest request) {
        if (expenseCategoryRepository.existsByName(request.getName()))
            throw new AppException(ErrorCode.CURRENCY_EXISTED);
        ExpenseCategory expenseCategory = expenseCategoryMapper.toCategory(request);
        return expenseCategoryMapper.toCategoryResponse(expenseCategoryRepository.save(expenseCategory));
    }

    public ExpenseCategoryResponse getCategory(Long id) {
        ExpenseCategory expenseCategory = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        return expenseCategoryMapper.toCategoryResponse(expenseCategory);
    }

    public List<ExpenseCategoryResponse> getAllCategories() {
        return expenseCategoryRepository.findAll().stream()
                .map(expenseCategoryMapper::toCategoryResponse)
                .toList();
    }
    public ExpenseCategoryResponse updateCategory(Long id, ExpenseCategoryUpdateRequest request) {
        ExpenseCategory expenseCategory = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        if (expenseCategoryRepository.existsByName(request.getName()))
            throw new AppException(ErrorCode.CURRENCY_EXISTED);
        expenseCategory.setName(request.getName());
        expenseCategory.setDescription(request.getDescription());
        return expenseCategoryMapper.toCategoryResponse(expenseCategoryRepository.save(expenseCategory));
    }
    public void deleteCategory(Long id) {
        ExpenseCategory expenseCategory = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        expenseCategoryRepository.delete(expenseCategory);
    }
}
