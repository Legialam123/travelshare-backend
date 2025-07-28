package com.TravelShare.service;

import com.TravelShare.dto.request.ChatRequest;
import com.TravelShare.dto.request.OCRRequest;
import com.TravelShare.dto.response.OCRResponse;
import com.TravelShare.dto.response.SimpleChatResponse;
import com.TravelShare.service.ImageOptimizationService.OptimizedImage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

@Service
@Slf4j
public class AIService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ImageOptimizationService imageOptimizationService;

    public AIService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, ImageOptimizationService imageOptimizationService) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.imageOptimizationService = imageOptimizationService;
    }


    public SimpleChatResponse chat(ChatRequest request) {
        try {
            ChatResponse response = chatClient
                    .prompt(request.message())
                    .call()
                    .chatResponse();

            log.info("AI response generated successfully");

            return SimpleChatResponse.builder()
                    .content(response.getResult().getOutput().getText())
                    .model(response.getMetadata().getModel())
                    .totalTokens(response.getMetadata().getUsage().getTotalTokens())
                    .messageId(response.getMetadata().getId())
                    .build();

        } catch (Exception e) {
            log.error("Error processing AI request", e);
            throw new RuntimeException("Không thể xử lý yêu cầu AI: " + e.getMessage());
        }
    }
    public OCRResponse processReceiptImage(OCRRequest request) {
        try {
            // Validate image
            if (!imageOptimizationService.isValidImage(request.file())) {
                throw new IllegalArgumentException("File không phải là hình ảnh hợp lệ");
            }
            
            if (!imageOptimizationService.isValidSize(request.file())) {
                throw new IllegalArgumentException("Kích thước file quá lớn (tối đa 5MB)");
            }

            // Optimize image
            OptimizedImage optimizedImage = imageOptimizationService.optimizeImage(request.file());
            
            // Use cache with optimized image
            return processReceiptImageWithCache(optimizedImage);

        } catch (Exception e) {
            log.error("Error in OCR processing: ", e);
            throw new RuntimeException("OCR processing failed", e);
        }
    }

    @Cacheable(value = "ocr-cache", key = "#optimizedImage.cacheKey")
    public OCRResponse processReceiptImageWithCache(OptimizedImage optimizedImage) {
        try {
            log.info("Processing OCR with cache key: {}", optimizedImage.getCacheKey());
            
            // Create Resource from optimized image
            Resource imageResource = new InputStreamResource(optimizedImage.getImageStream());
            
            // Detect MimeType (assume JPEG after optimization)
            MimeType mimeType = MimeTypeUtils.parseMimeType("image/jpeg");

            // Create Media object
            Media media = new Media(mimeType, imageResource);

            // Create prompt
            String promptText = createIntelligentOCRPrompt();

            // Use ChatClient fluent API
            String response = chatClient.prompt()
                    .user(u -> u.text(promptText).media(media))
                    .call()
                    .content();

            log.info("OCR Response: {}", response);
            return parseOCRResponse(response);

        } catch (Exception e) {
            log.error("Error in cached OCR processing: ", e);
            throw new RuntimeException("OCR processing failed", e);
        }
    }

    private String createIntelligentOCRPrompt() {
        return """
            Extract receipt info as JSON:
            {
                "merchantName": "store name",
                "amount": 0.0,
                "date": "2024-01-01 12:00:00",
                "description": "brief description",
                "categoryName": "category"
            }
            
            Categories: "Ăn uống", "Đi lại & Phương tiện", "Chỗ ở & Lưu trú", "Mua sắm", "Giải trí", "Khác"
            
            Return only valid JSON.
            """;
    }

    private OCRResponse parseOCRResponse(String response) {
        try {
            // Clean response (remove markdown formatting if any)
            String cleanJson = response.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            OCRResponse ocrResponse = objectMapper.readValue(cleanJson, OCRResponse.class);

            // Validate category name - đảm bảo category tồn tại trong system
            String validatedCategory = validateAndMapCategory(ocrResponse.categoryName());

            // Return với category đã được validate và default values cho confidence/originalText
            return new OCRResponse(
                    ocrResponse.merchantName(),
                    ocrResponse.amount(),
                    ocrResponse.date(),
                    ocrResponse.description(),
                    validatedCategory
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to parse OCR response: {}", response, e);
            throw new RuntimeException("Failed to parse OCR response", e);
        }
    }

    /**
     * Validate và map category name với system categories
     */
    private String validateAndMapCategory(String detectedCategory) {
        if (detectedCategory == null) return "Khác";

        // Exact matches
        String[] validCategories = {
                "Ăn uống",
                "Đi lại & Phương tiện",
                "Chỗ ở & Lưu trú",
                "Mua sắm",
                "Giải trí",
                "Khác"
        };

        // Check exact match first
        for (String validCategory : validCategories) {
            if (validCategory.equals(detectedCategory)) {
                return validCategory;
            }
        }

        // Fuzzy matching for common variations
        String lowerDetected = detectedCategory.toLowerCase();

        if (lowerDetected.contains("ăn") || lowerDetected.contains("uống") ||
                lowerDetected.contains("food") || lowerDetected.contains("restaurant")) {
            return "Ăn uống";
        }

        if (lowerDetected.contains("taxi") || lowerDetected.contains("grab") ||
                lowerDetected.contains("transport") || lowerDetected.contains("phương tiện")) {
            return "Đi lại & Phương tiện";
        }

        if (lowerDetected.contains("hotel") || lowerDetected.contains("khách sạn") ||
                lowerDetected.contains("accommodation") || lowerDetected.contains("lưu trú")) {
            return "Chỗ ở & Lưu trú";
        }

        if (lowerDetected.contains("shop") || lowerDetected.contains("mua") ||
                lowerDetected.contains("shopping") || lowerDetected.contains("siêu thị")) {
            return "Mua sắm";
        }

        if (lowerDetected.contains("entertain") || lowerDetected.contains("giải trí") ||
                lowerDetected.contains("cinema") || lowerDetected.contains("movie")) {
            return "Giải trí";
        }

        // Default fallback
        log.warn("Unknown category detected: {}, falling back to 'Khác'", detectedCategory);
        return "Khác";
    }
}
