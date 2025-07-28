package com.TravelShare.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OCRResponse(
        String merchantName,        // Tên cửa hàng
        BigDecimal amount,          // Số tiền
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime date,         // Ngày giao dịch
        String description,         // Mô tả chi tiêu
        String categoryName        // TÊN CATEGORY tự động nhận diện
) {
}
