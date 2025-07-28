package com.TravelShare.dto.response;

import lombok.Builder;

@Builder
public record SimpleChatResponse(
        String content,
        String model,
        Integer totalTokens,
        String messageId
) {
}
