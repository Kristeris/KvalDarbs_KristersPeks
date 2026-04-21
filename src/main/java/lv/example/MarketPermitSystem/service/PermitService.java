package lv.example.MarketPermitSystem.service;
 
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.repo.PermitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
@Service
public class PermitService {
 
    @Autowired
    private PermitRepository permitRepository;
 
    @Autowired
    private EmailService emailService;
 
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
 
    public Permit createPermit(String title, String description, String tradeLocation,
                               String tradeStartDate, String tradeEndDate,
                               List<MultipartFile> documents, MyUser user) throws IOException {
        Permit permit = new Permit(title, description, tradeLocation, tradeStartDate, tradeEndDate, user);
 
        if (documents != null && !documents.isEmpty()) {
            List<String> entries = new ArrayList<>();
            for (MultipartFile doc : documents) {
                if (doc != null && !doc.isEmpty()) {
                    String storedName = storeFile(doc);
                    String origName = doc.getOriginalFilename();
                    entries.add(origName + "::" + storedName);
                }
            }
            if (!entries.isEmpty()) {
                permit.setDocumentFiles(String.join(",", entries));
            }
        }
 
        Permit saved = permitRepository.save(permit);
        emailService.sendPermitSubmittedEmail(user, saved);
        return saved;
    }
 
    private String storeFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = uploadPath.resolve(uniqueName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return uniqueName;
    }


    public Path getFilePath(String storedName) {
        return Paths.get(uploadDir).resolve(storedName);
    }

    public List<Permit> getUserPermits(MyUser user) {
        return permitRepository.findByUserOrderBySubmittedAtDesc(user);
    }
 
    public List<Permit> getAllPermits() {
        return permitRepository.findAllByOrderBySubmittedAtDesc();
    }
 
    public Permit getPermitById(long id) {
        return permitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pieteikums nav atrasts"));
    }
 
    public Permit updateStatus(long permitId, PermitStatus newStatus, String adminComment) {
        Permit permit = getPermitById(permitId);
        permit.setStatus(newStatus);
        if (adminComment != null && !adminComment.isEmpty()) {
            permit.setAdminComment(adminComment);
        }
        Permit saved = permitRepository.save(permit);
        emailService.sendStatusChangedEmail(permit.getUser(), saved);
        return saved;
    }
 
    public long countByStatus(PermitStatus status) {
        return permitRepository.countByStatus(status);
    }
}
