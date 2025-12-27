package tn.enit.mail_service.services.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tn.enit.mail_service.models.Mail;
import tn.enit.mail_service.repositories.MailRepository;
import tn.enit.mail_service.services.interfaces.IEmailSender;
import tn.enit.mail_service.services.interfaces.IMailProcessor;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailProcessorImpl implements IMailProcessor {

    private final MailRepository mailRepository;
    private final IEmailSender emailSender;

    @Override
    public void processUnsentEmails() {
        List<Mail> unsentMails = mailRepository.findBySentFalse();

        if (unsentMails.isEmpty()) {
            log.debug("No emails to process");
            return;
        }

        log.info("Processing {} unsent emails", unsentMails.size());

        for (Mail mail : unsentMails) {
            processSingleEmail(mail);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSingleEmail(Mail mail) {
        if (mail.getRetryCount() >= mail.getMaxRetries()) {
            log.warn("Email ID:{} exceeded max retries ({}), skipping", mail.getId(), mail.getMaxRetries());
            return;
        }

        try {
            emailSender.sendEmail(mail);
            mail.setSent(true);
            mail.setSentAt(LocalDateTime.now());
            mailRepository.save(mail);
            log.info("Email processed successfully (ID: {})", mail.getId());
        } catch (Exception e) {
            mail.setRetryCount(mail.getRetryCount() + 1);
            mail.setLastRetryAt(LocalDateTime.now());
            mail.setErrorMessage(e.getMessage());
            mailRepository.save(mail);
            log.error("Failed to process email (ID: {}, retry: {}/{}): {}",
                    mail.getId(), mail.getRetryCount(), mail.getMaxRetries(), e.getMessage());
        }
    }
}
