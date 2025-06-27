package com.TravelShare.service;

import com.TravelShare.dto.request.ForgotPasswordRequest;
import com.TravelShare.dto.request.ResetPasswordRequest;
import com.TravelShare.entity.User;
import com.TravelShare.entity.UserToken;
import com.TravelShare.exception.AppException;
import com.TravelShare.exception.ErrorCode;
import com.TravelShare.repository.UserRepository;
import com.TravelShare.repository.UserTokenRepository;
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
import org.springframework.ui.Model;

import java.time.Duration;
import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailService {
    JavaMailSender mailSender;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserTokenService userTokenService;
    UserTokenRepository userTokenRepository;

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
    public void sendInvitationEmail(String toEmail, String groupName, String inviteLink) {
        String subject = "Bạn được mời tham gia nhóm " + groupName + " trên IShareMoney";
        String htmlMsg = "<h3>Xin chào!</h3>"
                + "<p>Bạn được mời tham gia nhóm <b>\"" + groupName + "\"</b> trên IShareMoney.</p>"
                + "<p>Nhấn vào liên kết bên dưới để đăng ký hoặc tham gia nhóm:</p>"
                + "<p><a href=\"" + inviteLink + "\">Tham gia nhóm</a></p>"
                + "<p>Nếu bạn không quan tâm, hãy bỏ qua email này.</p>"
                + "<p>Trân trọng,<br />Đội ngũ IShareMoney</p>";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            log.info("Invitation email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send invitation email to {}: {}", toEmail, e.getMessage());
        }
    }
}
