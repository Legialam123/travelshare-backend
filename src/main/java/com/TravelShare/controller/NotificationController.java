package com.TravelShare.controller;

import com.TravelShare.dto.request.NotificationCreationRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.dto.response.NotificationResponse;
import com.TravelShare.entity.Notification;
import com.TravelShare.entity.User;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.service.NotificationService;
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
@RequestMapping("/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationController {
    final NotificationService notificationService;
    final UserRepository userRepository;

    @GetMapping("/group/{groupId}")
    public ApiResponse<List<NotificationResponse>> getNotificationsByGroup(@PathVariable Long groupId) {
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notificationService.getNotificationsByGroup(groupId))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notificationService.getNotificationsByUser(user, groupId, type, fromDate, toDate))
                .build();
    }

    @PostMapping
    public ApiResponse<NotificationResponse> createNotification(@RequestBody NotificationCreationRequest request) {
        // Lấy user hiện tại từ SecurityContextHolder
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.createNotification(request, creator))
                .build();
    }
}
