package com.TravelShare.controller;

import com.TravelShare.dto.request.ForgotPasswordRequest;
import com.TravelShare.dto.request.ResetPasswordRequest;
import com.TravelShare.dto.response.ApiResponse;
import com.TravelShare.entity.PasswordResetToken;
import com.TravelShare.entity.User;
import com.TravelShare.entity.VerificationEmailToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.PasswordResetTokenRepository;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.repository.VerificationEmailTokenRepository;
import com.TravelShare.service.EmailService;
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
    VerificationEmailTokenRepository verificationTokenRepository;
    UserRepository userRepository;
    EmailService emailService;
    PasswordResetTokenRepository passwordResetTokenRepository;

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            VerificationEmailToken verificationToken = verificationTokenRepository.findByToken(token)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

            if (verificationToken.getExpiryTime().isBefore(LocalDateTime.now())) {
                model.addAttribute("message", "Link xác thực đã hết hạn!");
                model.addAttribute("success", false);
                return "user_verification";
            }

            User user = verificationToken.getUser();
            user.setActive(true);
            userRepository.save(user);

            // Xóa token sau khi xác thực thành công
            verificationTokenRepository.delete(verificationToken);

            model.addAttribute("message", "Xác thực tài khoản thành công! Bây giờ bạn có thể đăng nhập.");
            model.addAttribute("success", true);
            return "user_verification";
        } catch (Exception e) {
            model.addAttribute("message", "Xác thực thất bại: " + e.getMessage());
            model.addAttribute("success", false);
            return "user_verification";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        try {
            PasswordResetToken passwordResetToken = passwordResetTokenRepository.findById(token)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
            // Add this line to put the token in the model
            model.addAttribute("token", token);
            if(passwordResetToken.getExpiredAt().isBefore(LocalDateTime.now())){
                model.addAttribute("message", "Link đặt lại mật khẩu đã hết hạn!");
                model.addAttribute("success", false);
                return "reset_password";
            }
            return "reset_password";
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());
            return "reset_password_error";
        }
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        emailService.sendResetPasswordLink(request);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@ModelAttribute ResetPasswordRequest request, Model model) {
        try {
            emailService.resetPassword(request);
            model.addAttribute("success", true);
            model.addAttribute("message", "Mật khẩu đã được đặt lại thành công!");
            return "reset_password_success";
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());
            model.addAttribute("token", request.getToken());
            return "reset_password";
        }
    }
}
