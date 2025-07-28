package com.TravelShare.dto.request;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record OCRRequest(MultipartFile file)
{}