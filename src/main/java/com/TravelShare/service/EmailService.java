package com.TravelShare.service;

import com.TravelShare.dto.request.ForgotPasswordRequest;
import com.TravelShare.dto.request.ResetPasswordRequest;
import com.TravelShare.entity.PasswordResetToken;
import com.TravelShare.entity.User;
import com.TravelShare.entity.VerificationEmailToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.PasswordResetTokenRepository;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.repository.VerificationEmailTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailService {
    JavaMailSender mailSender;
    VerificationEmailTokenRepository verificationEmailTokenRepository;
    UserRepository userRepository;
    PasswordResetTokenRepository passwordResetTokenRepository;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${app.email.from}")
    String fromEmail;

    @NonFinal
    @Value("${app.email.verification-base-url}")
    String verificationBaseUrl;

    @NonFinal
    @Value("${app.email.reset-password-base-url}")
    String resetPasswordBaseUrl;

    @NonFinal
    @Value("${app.email.verification-expiration}")
    int verificationExpHours;

    @Async
    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        VerificationEmailToken verificationEmailToken = VerificationEmailToken.builder()
                .token(token)
                .user(user)
                .createdAt(now)
                .expiryTime(now.plusHours(verificationExpHours))
                .build();
        verificationEmailTokenRepository.save(verificationEmailToken);

        String verificationLink = verificationBaseUrl + "?token=" + verificationEmailToken.getToken();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("TravelShare - Xác thực tài khoản");

            String htmlMsg = "<h3>Xin chào " + user.getFullName() + "!</h3>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản tại TravelShare. "
                    + "Vui lòng nhấn vào liên kết bên dưới để xác thực tài khoản của bạn:</p>"
                    + "<p><a href=\"" + verificationLink + "\">Xác thực tài khoản</a></p>"
                    + "<p>Liên kết này sẽ hết hạn sau " + verificationExpHours + " giờ.</p>"
                    + "<p>Trân trọng,<br />Đội ngũ TravelShare</p>";

            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);

            log.info("Verification email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendResetPasswordLink(ForgotPasswordRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiredAt = LocalDateTime.now().plusHours(verificationExpHours);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(user.getEmail())
                .expiredAt(expiredAt)
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        //Send Reset Password Link to email User
        String resetLink = resetPasswordBaseUrl + "?token=" + resetToken.getToken();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("TravelShare - Yêu cầu đặt lại mật khẩu");

            String htmlMsg = "<h3>Xin chào " + user.getFullName() + "!</h3>"
                    + "<p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản của mình tại TravelShare. "
                    + "Vui lòng nhấn vào liên kết bên dưới để đặt lại mật khẩu của bạn:</p>"
                    + "<p><a href=\"" + resetLink + "\">Đặt lại mật khẩu</a></p>"
                    + "<p>Liên kết này sẽ hết hạn sau " + verificationExpHours + " giờ.</p>"
                    + "<p>Trân trọng,<br />Đội ngũ TravelShare</p>";

            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);

            log.info("Reset password email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send Reset password email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void resetPassword(ResetPasswordRequest request){
        PasswordResetToken resetToken = passwordResetTokenRepository.findById(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        if (resetToken.isUsed()) {
            throw new AppException(ErrorCode.TOKEN_USED);
        }

        if (resetToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
