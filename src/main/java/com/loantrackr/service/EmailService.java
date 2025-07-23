package com.loantrackr.service;

import com.loantrackr.util.TemplateUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from.email}")
    private String fromEmail;
    @Value("${mail.from.name}")
    private String fromName;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
        } catch (Exception e) {
            log.error("Exception in sendEmail ", e);
            throw e;
        }
    }


    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            helper.setFrom(fromEmail, fromName);

            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception ex) {
            log.error("Unexpected error while sending email", ex);
            throw new RuntimeException("Unexpected error while sending email" + ex.getMessage());
        }
    }

    public void sendLenderCredentials(String email, String username, String password) {

        String subject = "LoanTrackr | Your Lender Account Has Been Approved – Login Credentials Enclosed";

        Map<String, String> placeholders = Map.of(
                "email", email,
                "password", password,
                "organizationName", username
        );

        String html = TemplateUtil.loadTemplate("/templates/emails/lenderApproval.html", placeholders);

        sendHtmlEmail(email, subject, html);
    }

    public void sendLenderRejectionEmail(String email, String organizationName, String rejectionReason) {
        String subject = "LoanTrackr | Lender Onboarding Request Rejected";
        Map<String, String> replacements = Map.of(
                "organizationName", organizationName,
                "rejectionReason", rejectionReason
        );
        String html = TemplateUtil.loadTemplate("/templates/emails/lenderRejection.html", replacements);

        sendHtmlEmail(email, subject, html);
    }


    public void sendOtpEmail(String email, String otp) {
        String subject = "Your OTP for Verification";
        String body = """
                Dear user,
                
                Your One-Time Password (OTP) is: %s
                
                This OTP is valid for 10 minutes. Please do not share it with anyone.
                
                Regards,
                LoanTrackr Team
                """.formatted(otp);

        sendEmail(email, subject, body);
    }
}
