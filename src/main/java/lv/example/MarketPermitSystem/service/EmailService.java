package lv.example.MarketPermitSystem.service;
 
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.MyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
 
@Service
public class EmailService {
 
    @Autowired
    private JavaMailSender mailSender;
 
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("kristers1906@gmail.com");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("E-pasta sūtīšana neizdevās: " + e.getMessage());
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
}