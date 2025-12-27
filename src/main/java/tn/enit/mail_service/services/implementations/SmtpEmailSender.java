package tn.enit.mail_service.services.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tn.enit.mail_service.models.Mail;
import tn.enit.mail_service.services.interfaces.IEmailSender;

@Service("smtpSender")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements IEmailSender {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(Mail mail) throws Exception {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mail.getSenderEmail());
            message.setTo(mail.getRecipientEmail());
            message.setSubject(mail.getSubject());
            message.setText(mail.getBody());

            mailSender.send(message);
            log.info("Email envoyé via SMTP standard (ID: {})", mail.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail (ID: {}): {}", mail.getId(), e.getMessage());
            throw e;
        }
    }
}
