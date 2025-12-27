package tn.enit.mail_service.services.interfaces;

import tn.enit.mail_service.models.Mail;

public interface IEmailSender {
    void sendEmail(Mail mail) throws Exception;
}
