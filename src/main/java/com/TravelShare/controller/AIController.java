package com.TravelShare.controller;

import com.TravelShare.dto.request.ChatRequest;
import com.TravelShare.dto.request.OCRRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.OCRResponse;
import com.TravelShare.dto.response.SimpleChatResponse;
import com.TravelShare.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {
    private final AIService aiService;

    /**
     * Chat endpoint - Text conversation với AI
     */
    @PostMapping("/chat")
    public ApiResponse<SimpleChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            SimpleChatResponse response = aiService.chat(request);
            return ApiResponse.<SimpleChatResponse>builder()
                    .result(response)
                    .build();
        } catch (Exception e) {
            log.error("Error in AI chat", e);
            return ApiResponse.<SimpleChatResponse>builder()
                    .message("Không thể xử lý yêu cầu AI: " + e.getMessage())
                    .build();
        }
    }


    /**
     * OCR endpoint - Process receipt image và extract thông tin
     */
    @PostMapping(value = "/ocr/process-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OCRResponse> processReceipt(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Processing OCR request for file: {} (size: {} bytes)", 
                    file.getOriginalFilename(), file.getSize());

            // Validate file
            if (file.isEmpty()) {
                return ApiResponse.<OCRResponse>builder()
                        .code(1002)
                        .message("File không được để trống")
                        .build();
            }

            // Create OCR request
            OCRRequest ocrRequest = new OCRRequest(file);

            // Process OCR (with optimization and caching)
            OCRResponse response = aiService.processReceiptImage(ocrRequest);

            return ApiResponse.<OCRResponse>builder()
                    .code(1000)
                    .result(response)
                    .message("OCR processing thành công")
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file: {}", e.getMessage());
            return ApiResponse.<OCRResponse>builder()
                    .code(1003)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error in OCR processing for file: {}", file.getOriginalFilename(), e);
            return ApiResponse.<OCRResponse>builder()
                    .code(1004)
                    .message("Không thể xử lý OCR: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Health check endpoint cho AI service
     */
    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.<String>builder()
                .code(1000)
                .result("AI Service is running")
                .message("Service hoạt động bình thường")
                .build();
    }

    /**
     * Cache statistics endpoint
     */
    @GetMapping("/cache/stats")
    public ApiResponse<String> getCacheStats() {
        return ApiResponse.<String>builder()
                .code(1000)
                .result("Cache statistics")
                .message("Xem logs để biết cache statistics")
                .build();
    }

}
