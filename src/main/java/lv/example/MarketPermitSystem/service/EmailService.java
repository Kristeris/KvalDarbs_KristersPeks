package lv.example.MarketPermitSystem.service;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
 
@Service
public class EmailService {
 
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
 
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom(fromEmail);
            mailSender.send(message);
            log.info("E-pasts nosūtīts uz '{}': '{}'", to, subject);
        } catch (Exception e) {
            System.err.println("E-pasta sūtīšana neizdevās: " + e.getMessage());
            log.error("E-pasta sūtīšana neizdevās uz '{}': {}", to, e.getMessage());
        }
    }
 
    public void sendRegistrationEmail(MyUser user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) return;
        String subject = "Veiksmīga reģistrācija - Ventspils Tirdzniecības Atļauju Sistēma";
        String body = "Labdien, " + user.getUsername() + "!\n\n" +
                "Jūs esat veiksmīgi reģistrējies Ventspils tirdzniecības atļauju sistēmā.\n\n" +
                "Tagad varat:\n" +
                "• Iesniegt tirdzniecības atļauju pieteikumus\n" +
                "• Sekot līdzi savu pieteikumu statusam\n" +
                "• Saņemt paziņojumus par izmaiņām\n\n" +
                "Lai pieslēgtos: https://ventspils-permits.lv\n\n" +
                "Ar cieņu,\nVentspils valstspilsētas pašvaldība";
        sendEmail(user.getEmail(), subject, body);
    }
 
    public void sendPermitSubmittedEmail(MyUser user, Permit permit) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) return;
        String subject = "Pieteikums saņemts - " + permit.getTitle();
        String body = "Labdien, " + user.getUsername() + "!\n\n" +
                "Jūsu tirdzniecības atļaujas pieteikums ir veiksmīgi iesniegts.\n\n" +
                "Pieteikuma informācija:\n" +
                "• Nosaukums: " + permit.getTitle() + "\n" +
                "• Tirdzniecības vieta: " + permit.getTradeLocation() + "\n" +
                "• Statuss: " + permit.getStatus().getDisplayName() + "\n" +
                "• Iesniegšanas laiks: " + permit.getSubmittedAt() + "\n\n" +
                "Pieteikums tiks izskatīts 5 darba dienu laikā.\n\n" +
                "Ar cieņu,\nVentspils valstspilsētas pašvaldība";
        sendEmail(user.getEmail(), subject, body);
    }
 
    public void sendStatusChangedEmail(MyUser user, Permit permit) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) return;
        String subject = "Pieteikuma statuss mainīts - " + permit.getTitle();
        String body = "Labdien, " + user.getUsername() + "!\n\n" +
                "Jūsu tirdzniecības atļaujas pieteikuma statuss ir mainīts.\n\n" +
                "Pieteikuma informācija:\n" +
                "• Nosaukums: " + permit.getTitle() + "\n" +
                "• Jaunais statuss: " + permit.getStatus().getDisplayName() + "\n";
        if (permit.getAdminComment() != null && !permit.getAdminComment().isEmpty()) {
            body += "• Administratora komentārs: " + permit.getAdminComment() + "\n";
        }
        body += "\nLūdzu, pieslēdzieties sistēmai, lai skatītu pilnu informāciju.\n\n" +
                "Ar cieņu,\nVentspils valstspilsētas pašvaldība";
        sendEmail(user.getEmail(), subject, body);
    }
    
    public void sendEdocEmail(MyUser user, Permit permit, MultipartFile edocFile)
            throws MessagingException {
 
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Lietotājam '{}' nav e-pasta adreses — eDoc netika nosūtīts", user.getUsername());
            throw new IllegalStateException(
                "Lietotājam nav norādīta e-pasta adrese. eDoc nevar nosūtīt.");
        }
 
        MimeMessage mime = mailSender.createMimeMessage();
        // true = multipart (pielikumiem)
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
 
        helper.setFrom(fromEmail);
        helper.setTo(user.getEmail());
        helper.setSubject("Tirdzniecības atļauja apstiprināta - " + permit.getTitle());
 
        String body =
            "Labdien, " + user.getUsername() + "!\n\n" +
            "Jūsu tirdzniecības atļaujas pieteikums ir apstiprināts un atļauja ir pievienota šim e-pastam.\n\n" +
            "Pieteikuma informācija:\n" +
            "• Nosaukums: " + permit.getTitle() + "\n" +
            "• Tirdzniecības vieta: " + permit.getTradeLocation() + "\n" +
            "• Periods: " + permit.getTradeStartDate() + " – " + permit.getTradeEndDate() + "\n\n" +
            "Lūdzu, glabājiet šo dokumentu — tas apliecina Jūsu tiesības veikt tirdzniecību.\n\n" +
            "Ar cieņu,\nVentspils valstspilsētas pašvaldība\n" +
            "Ekonomikas un iepirkumu nodaļa\n" +
            "Tālrunis: 63601191 | E-pasts: ekonomika@ventspils.lv";
 
        helper.setText(body);
 
        // Pievieno eDoc failu kā pielikumu
        try {
            helper.addAttachment(
                edocFile.getOriginalFilename() != null
                    ? edocFile.getOriginalFilename()
                    : "atlauja.edoc",
                new ByteArrayResource(edocFile.getBytes())
            );
        } catch (Exception e) {
            throw new MessagingException("Neizdevās pievienot failu e-pastam: " + e.getMessage());
        }
 
        mailSender.send(mime);
        log.info("eDoc e-pasts nosūtīts lietotājam '{}' uz '{}' par pieteikumu #{}",
                user.getUsername(), user.getEmail(), permit.getPId());
    }
}