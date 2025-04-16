package com.TravelShare.controller;

import com.TravelShare.entity.User;
import com.TravelShare.entity.VerificationEmailToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.repository.VerificationEmailTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailController {
    VerificationEmailTokenRepository verificationTokenRepository;
    UserRepository userRepository;

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
}
