package com.TravelShare.controller;

import com.TravelShare.dto.request.ForgotPasswordRequest;
import com.TravelShare.dto.request.ResetPasswordRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.entity.User;
import com.TravelShare.entity.UserToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.repository.UserTokenRepository;
import com.TravelShare.service.AuthenticationService;
import com.TravelShare.service.EmailService;
import com.TravelShare.service.UserService;
import com.TravelShare.service.UserTokenService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailController {
    UserRepository userRepository;
    EmailService emailService;
    UserTokenRepository userTokenRepository;
    UserTokenService userTokenService;
    UserService userService;

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        userTokenService.verifyEmailToken(token, model);
        return "user_verification";
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        emailService.sendResetPasswordLink(request);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        userTokenService.verifyResetToken(token, model);
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@ModelAttribute ResetPasswordRequest request, Model model) {
        boolean success = userService.processResetPassword(request, model);
        return success ? "reset_password_success" : "reset_password";
    }
}
