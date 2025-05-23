package com.TravelShare.mapper;

import com.TravelShare.dto.request.CategoryCreationRequest;
import com.TravelShare.dto.request.CategoryUpdateRequest;
import com.TravelShare.dto.response.CategoryResponse;
import com.TravelShare.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    
    Category toCategory(CategoryCreationRequest request);
    CategoryResponse toCategoryResponse(Category category);

    @Mapping(target = "id", ignore = true)
    void updateCategory(@MappingTarget Category category, CategoryUpdateRequest request);

}
