package com.TravelShare.dto.response;

import com.TravelShare.entity.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaResponse {
    Long id;
    String fileName;
    String originalFileName;
    Long fileSize;
    String fileUrl;
    String filePath;
    String contentType;
    Media.MediaType mediaType;
    String description;
    LocalDateTime uploadedAt;
    UserResponse uploadedBy;
}
