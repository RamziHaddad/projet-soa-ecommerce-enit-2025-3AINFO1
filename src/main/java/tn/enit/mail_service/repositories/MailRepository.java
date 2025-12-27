package tn.enit.mail_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.enit.mail_service.models.Mail;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {

    Optional<Mail> findByContentHash(String contentHash);

    List<Mail> findBySentFalse();
}
