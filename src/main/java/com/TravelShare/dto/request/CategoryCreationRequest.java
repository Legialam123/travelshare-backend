package com.TravelShare.dto.request;

import com.TravelShare.entity.Category;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryCreationRequest {
    String name;
    String description;
    Long groupId;
    String iconCode;
    String color;
    Category.CategoryType type;
}
