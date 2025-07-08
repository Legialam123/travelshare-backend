package com.TravelShare.service;

import com.TravelShare.dto.request.ForgotPasswordRequest;
import com.TravelShare.entity.User;
import com.TravelShare.entity.UserToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
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
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailService {
    JavaMailSender mailSender;
    UserRepository userRepository;
    UserTokenService userTokenService;

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
    Duration verificationExpiration;

    @Async
    public void sendVerificationEmail(User user) {
        UserToken userToken = userTokenService.createToken(user, UserToken.TokenType.VERIFY_EMAIL,verificationExpiration);

        String verificationLink = verificationBaseUrl + "?token=" + userToken.getToken();

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
                    + "<p>Liên kết này sẽ hết hạn sau " + verificationExpiration.toHours() + " giờ.</p>"
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
        UserToken userToken = userTokenService.createToken(user, UserToken.TokenType.RESET_PASSWORD, verificationExpiration);
        //Send Reset Password Link to email User
        String resetLink = resetPasswordBaseUrl + "?token=" + userToken.getToken();
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
                    + "<p>Liên kết này sẽ hết hạn sau " + verificationExpiration.toHours() + " giờ.</p>"
                    + "<p>Trân trọng,<br />Đội ngũ TravelShare</p>";

            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);

            log.info("Reset password email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send Reset password email to {}: {}", user.getEmail(), e.getMessage());
        }
    }


    @Async
    public void sendJoinCodeEmail(String toEmail, String groupName, String joinCode) {
        String subject = "Mã tham gia nhóm " + groupName + " - IShareMoney";
        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h3 style='color: #2563eb;'>Xin chào!</h3>"
                + "<p>Bạn được mời tham gia nhóm <b>\"" + groupName + "\"</b> trên IShareMoney.</p>"
                + "<div style='background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;'>"
                + "<h4 style='margin: 0 0 10px 0; color: #374151;'>Mã tham gia:</h4>"
                + "<p style='font-size: 24px; font-weight: bold; color: #dc2626; margin: 0; letter-spacing: 2px;'>"
                + joinCode + "</p>"
                + "</div>"
                + "<h4 style='color: #374151;'>Hướng dẫn tham gia:</h4>"
                + "<ol style='color: #6b7280; line-height: 1.6;'>"
                + "<li>Tải ứng dụng <strong>IShareMoney</strong> (nếu chưa có)</li>"
                + "<li>Đăng ký hoặc đăng nhập tài khoản</li>"
                + "<li>Vào mục <strong>\"Tham gia nhóm có sẵn\"</strong></li>"
                + "<li>Nhập mã: <strong style='color: #dc2626;'>" + joinCode + "</strong></li>"
                + "<li>Chờ admin xác nhận yêu cầu của bạn</li>"
                + "</ol>"
                + "<div style='background-color: #fef3c7; padding: 15px; border-radius: 6px; border-left: 4px solid #f59e0b; margin: 20px 0;'>"
                + "<p style='margin: 0; color: #92400e;'>"
                + "<strong>Lưu ý:</strong> Yêu cầu tham gia sẽ cần được admin phê duyệt trước khi bạn có thể sử dụng nhóm."
                + "</p>"
                + "</div>"
                + "<p style='color: #6b7280;'>Nếu bạn không quan tâm, hãy bỏ qua email này.</p>"
                + "<hr style='border: 0; height: 1px; background: #e5e7eb; margin: 30px 0;'>"
                + "<p style='color: #9ca3af; font-size: 14px;'>Trân trọng,<br />Đội ngũ IShareMoney</p>"
                + "</div>";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            log.info("Join code email sent to: {} for group: {} with code: {}", toEmail, groupName, joinCode);
        } catch (MessagingException e) {
            log.error("Failed to send join code email to {}: {}", toEmail, e.getMessage());
        }
    }

}
