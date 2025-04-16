package com.TravelShare.service;

import com.TravelShare.entity.User;
import com.TravelShare.entity.VerificationEmailToken;
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

    @NonFinal
    @Value("${app.email.from}")
    String fromEmail;

    @NonFinal
    @Value("${app.email.verification-base-url}")
    String verificationBaseUrl;

    @NonFinal
    @Value("${app.email.verification-expiration}")
    int verificationExpHours;

    public VerificationEmailToken createVerificationEmailToken(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        VerificationEmailToken verificationEmailToken = VerificationEmailToken.builder()
                .token(token)
                .user(user)
                .createdAt(now)
                .expiryTime(now.plusHours(verificationExpHours))
                .build();

        return verificationEmailTokenRepository.save(verificationEmailToken);
    }

    @Async
    public void sendVerificationEmail(User user) {
        VerificationEmailToken verificationEmailToken = createVerificationEmailToken(user);
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
}
