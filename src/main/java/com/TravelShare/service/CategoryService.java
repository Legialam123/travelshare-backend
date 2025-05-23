package com.TravelShare.service;

import com.TravelShare.dto.request.CategoryCreationRequest;
import com.TravelShare.dto.request.CategoryUpdateRequest;
import com.TravelShare.dto.response.CategoryResponse;
import com.TravelShare.entity.Category;
import com.TravelShare.entity.Group;
import com.TravelShare.entity.GroupParticipant;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.mapper.CategoryMapper;
import com.TravelShare.repository.CategoryRepository;
import com.TravelShare.repository.GroupParticipantRepository;
import com.TravelShare.repository.GroupRepository;
import com.TravelShare.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CategoryService {
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    GroupRepository groupRepository;
    UserRepository userRepository;
    GroupParticipantRepository groupParticipantRepository;

    /**
     * Lấy thông tin người dùng hiện tại
     */
    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Lấy danh sách category dùng cho Group (type = GROUP hoặc BOTH)
     */
    public List<CategoryResponse> getGroupCategories() {
        return categoryRepository.findByTypeIn(List.of(Category.CategoryType.GROUP, Category.CategoryType.BOTH))
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    /**
     * Lấy danh sách category dùng cho Expense (type = EXPENSE hoặc BOTH)
     */
    public List<CategoryResponse> getExpenseCategories() {
        return categoryRepository.findByTypeIn(List.of(Category.CategoryType.EXPENSE, Category.CategoryType.BOTH))
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }
    
    /**
     * Lấy danh sách category EXPENSE của một group cụ thể
     */
    public List<CategoryResponse> getGroupExpenseCategories(Long groupId) {
        // Kiểm tra group tồn tại
        if (!groupRepository.existsById(groupId)) {
            throw new AppException(ErrorCode.GROUP_NOT_EXISTED);
        }
        
        // Lấy danh sách category EXPENSE hệ thống và các category EXPENSE của group
        return categoryRepository.findByTypeAndGroupIdOrTypeAndGroupIsNull(
                Category.CategoryType.EXPENSE, 
                groupId, 
                List.of(Category.CategoryType.EXPENSE, Category.CategoryType.BOTH))
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    /**
     * Tạo category mới (hệ thống hoặc tùy chỉnh)
     */
    public CategoryResponse createCategory(CategoryCreationRequest request, boolean isSystem) {
        // Kiểm tra trùng tên theo type
        if (categoryRepository.existsByNameAndTypeAndGroupIsNull(request.getName(), request.getType())) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }
        
        Category category = categoryMapper.toCategory(request);
        category.setIsSystemCategory(isSystem);
        
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }
    
    /**
     * Tạo category EXPENSE cho một group cụ thể
     */
    public CategoryResponse createExpenseCategoryForGroup(Long groupId, CategoryCreationRequest request) {
        // Lấy thông tin người dùng hiện tại
        User currentUser = getCurrentUser();
        String userId = currentUser.getId();
        log.info("User ID: {}", userId);
        
        // Kiểm tra user có trong group không
        GroupParticipant participant = groupParticipantRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_GROUP_MEMBER));
        
        // Kiểm tra group tồn tại
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_EXISTED));
        
        // Tự động gán type = EXPENSE
        request.setType(Category.CategoryType.EXPENSE);
        
        // Kiểm tra trùng tên trong group
        if (categoryRepository.existsByNameAndTypeAndGroupId(request.getName(), Category.CategoryType.EXPENSE, groupId)) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }
        
        Category category = categoryMapper.toCategory(request);
        category.setIsSystemCategory(false);
        category.setGroup(group);
        category.setCreatedBy(currentUser);
        
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    /**
     * Lấy thông tin chi tiết category
     */
    public CategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        return categoryMapper.toCategoryResponse(category);
    }

    /**
     * Cập nhật thông tin category EXPENSE (chỉ người tạo mới được phép)
     */
    public CategoryResponse updateExpenseCategory(Long id, CategoryUpdateRequest request) {
        // Lấy thông tin người dùng hiện tại
        User currentUser = getCurrentUser();
        String userId = currentUser.getId();
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        
        // Kiểm tra nếu là category hệ thống
        if (category.getIsSystemCategory()) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_SYSTEM_CATEGORY);
        }
        
        // Kiểm tra quyền (chỉ người tạo mới được cập nhật)
        if (category.getCreatedBy() == null || !category.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Kiểm tra loại category (chỉ được cập nhật EXPENSE)
        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_CATEGORY_TYPE);
        }
        
        // Kiểm tra trùng tên trong group
        if (!category.getName().equals(request.getName()) && 
                categoryRepository.existsByNameAndTypeAndGroupId(
                        request.getName(), Category.CategoryType.EXPENSE, category.getGroup().getId())) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIconCode(request.getIconCode());
        category.setColor(request.getColor());
        
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    /**
     * Xóa category EXPENSE (chỉ người tạo mới được phép)
     */
    public void deleteExpenseCategory(Long id) {
        // Lấy thông tin người dùng hiện tại
        User currentUser = getCurrentUser();
        String userId = currentUser.getId();
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXISTED));
        
        // Kiểm tra nếu là category hệ thống
        if (category.getIsSystemCategory()) {
            throw new AppException(ErrorCode.CANNOT_DELETE_SYSTEM_CATEGORY);
        }
        
        // Kiểm tra quyền (chỉ người tạo mới được xóa)
        if (category.getCreatedBy() == null || !category.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Kiểm tra loại category (chỉ được xóa EXPENSE)
        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new AppException(ErrorCode.CANNOT_DELETE_CATEGORY_TYPE);
        }
        
        categoryRepository.delete(category);
    }
}
