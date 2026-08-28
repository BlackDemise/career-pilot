package blackdemise.cp.user;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public void sendRegistrationVerification(String email, String token) {
        String link = buildLink(properties.registrationPath(), token);
        send(email, "Verify your CareerPilot account", "Open this link within 1 hour to verify your account:\n\n" + link);
    }

    public void sendPasswordReset(String email, String token) {
        String link = buildLink(properties.passwordResetPath(), token);
        send(email, "Reset your CareerPilot password", "Open this link within 1 hour to reset your password:\n\n" + link);
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    private String buildLink(String path, String token) {
        return properties.frontendBaseUrl() + path + "?token="
                + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
    }
}
