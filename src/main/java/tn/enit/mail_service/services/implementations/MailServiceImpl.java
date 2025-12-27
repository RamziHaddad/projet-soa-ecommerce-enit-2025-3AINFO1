package tn.enit.mail_service.services.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.enit.mail_service.dto.MailRequest;
import tn.enit.mail_service.dto.MailResponse;
import tn.enit.mail_service.models.Mail;
import tn.enit.mail_service.repositories.MailRepository;
import tn.enit.mail_service.services.interfaces.IMailService;
import tn.enit.mail_service.utils.MailHashUtil;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements IMailService {

    private final MailRepository mailRepository;

    @Override
    public MailResponse queueEmail(MailRequest request) {
        String hash = MailHashUtil.generateHash(
                request.getSenderEmail(),
                request.getRecipientEmail(),
                request.getSubject(),
                request.getBody());

        Optional<Mail> existingMail = mailRepository.findByContentHash(hash);

        if (existingMail.isPresent()) {
            log.info("Duplicate email detected (hash: {}), skipping", hash);
            return new MailResponse(true, "Email already queued", existingMail.get().getId());
        }

        Mail mail = new Mail();
        mail.setSenderEmail(request.getSenderEmail());
        mail.setRecipientEmail(request.getRecipientEmail());
        mail.setSubject(request.getSubject());
        mail.setBody(request.getBody());
        mail.setContentHash(hash);
        mail.setSent(false);

        Mail savedMail = mailRepository.save(mail);
        log.info("Email queued successfully (ID: {}, hash: {})", savedMail.getId(), hash);

        return new MailResponse(true, "Email queued successfully", savedMail.getId());
    }
}
