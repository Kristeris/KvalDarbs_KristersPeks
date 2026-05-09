package lv.example.MarketPermitSystem.service;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.repo.PermitRepository;
 
@Service
public class PermitService {

    private static final Logger log = LoggerFactory.getLogger(PermitService.class);
    private static final int PAGE_SIZE = 10;
 
    @Autowired
    private PermitRepository permitRepository;
 
    @Autowired
    private EmailService emailService;
 
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
 
    public Permit createPermit(String title, String description, String tradeLocation,
                               String tradeStartDate, String tradeEndDate,
                               List<MultipartFile> documents, MyUser user) throws IOException {
        
        LocalDate today = LocalDate.now();
 
        LocalDate startDate;
        LocalDate endDate;
 
        log.info("Lietotājs '{}' veido jaunu pieteikumu: '{}'", user.getUsername(), title);

        try {
            startDate = LocalDate.parse(tradeStartDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nepareizs sākuma datuma formāts.");
        }
 
        try {
            endDate = LocalDate.parse(tradeEndDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nepareizs beigu datuma formāts.");
        }
 
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException(
                "Tirdzniecības sākuma datums nevar būt pagātnē. Lūdzu, izvēlieties šodienas vai nākotnes datumu.");
        }
 
        if (endDate.isBefore(today)) {
            throw new IllegalArgumentException(
                "Tirdzniecības beigu datums nevar būt pagātnē. Lūdzu, izvēlieties šodienas vai nākotnes datumu.");
        }
 
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                "Tirdzniecības beigu datums nevar būt pirms sākuma datuma.");
        }
        Permit permit = new Permit(title, description, tradeLocation, tradeStartDate, tradeEndDate, user);
 
        if (documents != null && !documents.isEmpty()) {
            List<String> entries = new ArrayList<>();
            for (MultipartFile doc : documents) {
                if (doc != null && !doc.isEmpty()) {
                    String storedName = storeFile(doc);
                    entries.add(doc.getOriginalFilename() + "::" + storedName);
                }
            }
            if (!entries.isEmpty()) {
                permit.setDocumentFiles(String.join(",", entries));
            }
        }
 
        Permit saved = permitRepository.save(permit);
        log.info("Pieteikums #{} veiksmīgi izveidots lietotājam '{}'", saved.getPId(), user.getUsername());
        emailService.sendPermitSubmittedEmail(user, saved);
        return saved;
    }

    public Permit updatePermit(long permitId, String title, String description, String tradeLocation,
                               String tradeStartDate, String tradeEndDate,
                               List<MultipartFile> newDocuments,
                               List<String> removeDocuments,
                               MyUser requestingUser) throws IOException {
                
        Permit permit = getPermitById(permitId);

        log.info("Lietotājs '{}' rediģē pieteikumu #{}", requestingUser.getUsername(), permitId);
 
        // Only the owner may edit
        if (permit.getUser().getUId() != requestingUser.getUId()) {
            throw new IllegalArgumentException("Jums nav tiesību rediģēt šo pieteikumu.");
        }
 
        // Only editable in these two statuses
        if (permit.getStatus() != PermitStatus.IESNIEGTS
                && permit.getStatus() != PermitStatus.PAPILDINAJUMI_NEPIECIESAMI) {
            throw new IllegalArgumentException("Šo pieteikumu vairs nevar rediģēt.");
        }
 
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;
 
        try {
            startDate = LocalDate.parse(tradeStartDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nepareizs sākuma datuma formāts.");
        }
 
        try {
            endDate = LocalDate.parse(tradeEndDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nepareizs beigu datuma formāts.");
        }
 
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException(
                "Tirdzniecības sākuma datums nevar būt pagātnē.");
        }
 
        if (endDate.isBefore(today)) {
            throw new IllegalArgumentException(
                "Tirdzniecības beigu datums nevar būt pagātnē.");
        }
 
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                "Tirdzniecības beigu datums nevar būt pirms sākuma datuma.");
        }
 
        permit.setTitle(title);
        permit.setDescription(description);
        permit.setTradeLocation(tradeLocation);
        permit.setTradeStartDate(tradeStartDate);
        permit.setTradeEndDate(tradeEndDate);
 
        // Handle document removal
        List<String> currentEntries = new ArrayList<>();
        if (permit.getDocumentFiles() != null && !permit.getDocumentFiles().isEmpty()) {
            for (String entry : permit.getDocumentFiles().split(",")) {
                String[] parts = entry.split("::");
                if (parts.length == 2 && (removeDocuments == null || !removeDocuments.contains(parts[1]))) {
                    currentEntries.add(entry);
                }
            }
        }
 
        // Handle new document uploads
        if (newDocuments != null && !newDocuments.isEmpty()) {
            for (MultipartFile doc : newDocuments) {
                if (doc != null && !doc.isEmpty()) {
                    String storedName = storeFile(doc);
                    currentEntries.add(doc.getOriginalFilename() + "::" + storedName);
                }
            }
        }
 
        permit.setDocumentFiles(currentEntries.isEmpty() ? null : String.join(",", currentEntries));
 
        // If it was waiting for additions, reset to IESNIEGTS so admin sees the update
        if (permit.getStatus() == PermitStatus.PAPILDINAJUMI_NEPIECIESAMI) {
            permit.setStatus(PermitStatus.IESNIEGTS);
        }
 
        Permit saved = permitRepository.save(permit);
        emailService.sendPermitSubmittedEmail(requestingUser, saved);
        log.info("Pieteikums #{} atjaunināts, statuss: {}", saved.getPId(), saved.getStatus());
        return saved;
    }

    public void deletePermit(long permitId, MyUser requestingUser) {
        Permit permit = getPermitById(permitId);

        log.warn("Lietotājs '{}' dzēš pieteikumu #{}", requestingUser.getUsername(), permitId);
 
        if (permit.getUser().getUId() != requestingUser.getUId()) {
            throw new IllegalArgumentException("Jums nav tiesību dzēst šo pieteikumu.");
        }
 
        if (permit.getStatus() != PermitStatus.IESNIEGTS) {
            throw new IllegalArgumentException("Šo pieteikumu vairs nevar dzēst — statuss ir \"" +
                    permit.getStatus().getDisplayName() + "\".");
        }
 
        // Delete uploaded files from disk
        if (permit.getDocumentFiles() != null && !permit.getDocumentFiles().isEmpty()) {
            for (String entry : permit.getDocumentFiles().split(",")) {
                String[] parts = entry.split("::");
                if (parts.length == 2) {
                    try {
                        Path filePath = Paths.get(uploadDir).resolve(parts[1]);
                        Files.deleteIfExists(filePath);
                    } catch (IOException e) {
                        System.err.println("Neizdevās dzēst failu: " + parts[1] + " — " + e.getMessage());
                    }
                }
            }
        }
 
        permitRepository.delete(permit);
        log.info("Pieteikums #{} dzēsts", permitId);
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
    // Pagination metodes
 
    //Atgriež vienu lapas pieteikumus konkrētam lietotājam.
    public Page<Permit> getUserPermitsPaged(MyUser user, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return permitRepository.findByUserOrderBySubmittedAtDesc(user, pageable);
    }
 
    
    //Atgriež vienu lapas pieteikumus admin skatam.
    public Page<Permit> getAllPermitsPaged(int page, String searchUsername) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        if (searchUsername == null || searchUsername.isBlank()) {
            return permitRepository.findAllByOrderBySubmittedAtDesc(pageable);
        }
        return permitRepository.findByUserUsernameContainingIgnoreCaseOrderBySubmittedAtDesc(
                        searchUsername.trim(), pageable);
    }
 
    // Esošās metodes (statistikai un citur)
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
        log.info("Admins maina pieteikuma #{} statusu uz '{}'", permitId, newStatus);
        permit.setStatus(newStatus);
        if (adminComment != null && !adminComment.isEmpty()) {
            permit.setAdminComment(adminComment);
        }
        Permit saved = permitRepository.save(permit);
        emailService.sendStatusChangedEmail(permit.getUser(), saved);
        log.info("Pieteikums #{} statuss mainīts, e-pasts nosūtīts lietotājam '{}'",
                saved.getPId(), permit.getUser().getUsername());
        return saved;
    }
 
    public long countByStatus(PermitStatus status) {
        return permitRepository.countByStatus(status);
    }
}

