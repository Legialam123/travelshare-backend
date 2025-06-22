package com.TravelShare.controller;


import com.TravelShare.dto.request.RequestCreationRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.RequestResponse;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.service.RequestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/request")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RequestController {
    RequestService requestService;
    UserRepository userRepository;

    @GetMapping("/my")
    public ApiResponse<List<RequestResponse>> getMyRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User receiver = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<List<RequestResponse>>builder()
                .result(requestService.getMyRequests(receiver))
                .build();
    }

    @GetMapping("/sent")
    public ApiResponse<List<RequestResponse>> getMySentRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<List<RequestResponse>>builder()
                .result(requestService.getRequestsBySender(sender))
                .build();
    }

    @PostMapping
    public ApiResponse<RequestResponse> createRequest(@RequestBody RequestCreationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<RequestResponse>builder()
                .result(requestService.createRequest(request, sender))
                .build();
    }

    @PostMapping("/{id}/payment-confirm")
    public ApiResponse<RequestResponse> sendPaymentConfirm(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<RequestResponse>builder()
                .result(requestService.sendPaymentConfirm(id, sender))
                .build();
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<RequestResponse> acceptRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User receiver = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<RequestResponse>builder()
                .result(requestService.acceptRequest(id, receiver))
                .build();
    }

    @PostMapping("/{id}/decline")
    public ApiResponse<RequestResponse> declineRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User receiver = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<RequestResponse>builder()
                .result(requestService.declineRequest(id, receiver))
                .build();
    }

    @DeleteMapping("/{id}/delete")
    public ApiResponse<String> deleteRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        requestService.deleteRequest(id, user);
        return ApiResponse.<String>builder()
                .result("Yêu cầu đã được xóa bỏ !")
                .build();
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<String> cancelRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        requestService.cancelRequest(id, user);
        return ApiResponse.<String>builder()
                .result("Yêu cầu đã được hủy bỏ !")
                .build();
    }

}
