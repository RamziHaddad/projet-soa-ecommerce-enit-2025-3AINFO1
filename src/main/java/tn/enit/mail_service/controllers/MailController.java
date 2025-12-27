package tn.enit.mail_service.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.enit.mail_service.dto.MailRequest;
import tn.enit.mail_service.dto.MailResponse;
import tn.enit.mail_service.services.interfaces.IMailService;

@RestController
@RequestMapping("/api/v1/mails")
@RequiredArgsConstructor
public class MailController {

    private final IMailService mailService;

    @PostMapping
    public ResponseEntity<MailResponse> queueEmail(@Valid @RequestBody MailRequest request) {
        MailResponse response = mailService.queueEmail(request);
        HttpStatus status = response.getMessage().contains("already") ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
