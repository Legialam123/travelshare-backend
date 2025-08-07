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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/my/filter")
    public ApiResponse<List<RequestResponse>> getMyRequestsWithFilter(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "all") String direction
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User receiver = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<List<RequestResponse>>builder()
                .result(requestService.getMyRequestsWithFilter(receiver, groupId, type, fromDate, toDate, direction))
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

    @GetMapping("/sent/filter")
    public ApiResponse<List<RequestResponse>> getMySentRequestsWithFilter(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<List<RequestResponse>>builder()
                .result(requestService.getRequestsBySenderWithFilter(sender, groupId, type, fromDate, toDate))
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
    public ApiResponse<Void> sendPaymentConfirm(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        requestService.sendPaymentConfirm(id, user);
        return ApiResponse.<Void>builder().build();
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

    @PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancelRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        requestService.cancelRequest(id, user);
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        requestService.deleteRequest(id, user);
        return ApiResponse.<Void>builder().build();
    }
}
