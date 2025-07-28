package com.TravelShare.dto.request;

import lombok.Builder;

@Builder
public record ChatRequest(String message) {
}
