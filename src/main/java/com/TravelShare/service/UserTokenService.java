package com.TravelShare.service;

import com.TravelShare.entity.User;
import com.TravelShare.entity.UserToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.repository.UserTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import java.time.Duration;
import java.time.LocalDateTime;
import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserTokenService {
    UserTokenRepository userTokenRepository;
    UserRepository userRepository;

    public UserToken createToken(User user, UserToken.TokenType type, Duration validDuration) {
        UserToken userToken = UserToken.builder()
                .user(user)
                .type(type)
                .expiredAt(now().plus(validDuration))
                .build();
        return userTokenRepository.save(userToken);
    }

    public UserToken checkToken(String token) {
        UserToken userToken = userTokenRepository.findById(token)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_NOT_EXISTED));
        if (userToken.isUsed()) {
            throw new AppException(ErrorCode.TOKEN_USED);
        }
        if (userToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }
        return userToken;
    }

    public UserToken verifyToken(String token) {
        UserToken userToken = checkToken(token);
        userToken.setUsed(true);
        userTokenRepository.save(userToken);
        return userToken;
    }

    public void verifyEmailToken(String token, Model model) {
        try {
            UserToken verificationToken = verifyToken(token);

            if (verificationToken.getType() != UserToken.TokenType.VERIFY_EMAIL) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }

            User user = verificationToken.getUser();
            user.setActive(true);
            userRepository.save(user);

            model.addAttribute("message", "Xác thực tài khoản thành công! Bây giờ bạn có thể đăng nhập.");
            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("message", "Xác thực thất bại: " + e.getMessage());
            model.addAttribute("success", false);
        }
    }

    public void verifyResetToken(String token, Model model) {
        try {
            UserToken resetToken = checkToken(token);
            model.addAttribute("token", token);

            if (resetToken.getType() != UserToken.TokenType.RESET_PASSWORD)
                throw new AppException(ErrorCode.INVALID_KEY);

            model.addAttribute("success", true);
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());
        }
    }
}
