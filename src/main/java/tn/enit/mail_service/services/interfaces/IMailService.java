package tn.enit.mail_service.services.interfaces;

import tn.enit.mail_service.dto.MailRequest;
import tn.enit.mail_service.dto.MailResponse;

public interface IMailService {
    MailResponse queueEmail(MailRequest request);
}
