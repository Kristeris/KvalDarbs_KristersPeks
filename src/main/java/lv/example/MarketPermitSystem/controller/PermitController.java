package lv.example.MarketPermitSystem.controller;
 
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.service.PermitService;
import lv.example.MarketPermitSystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
 
@Controller
public class PermitController {
 
    @Autowired
    private PermitService permitService;
 
    @Autowired
    private UserService userService;
 
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        MyUser user = userService.findByUsername(userDetails.getUsername());
        List<Permit> permits = permitService.getUserPermits(user);
        model.addAttribute("user", user);
        model.addAttribute("permits", permits);
        model.addAttribute("statuses", PermitStatus.values());
 
        
        long submitted = permits.stream().filter(p -> p.getStatus() == PermitStatus.IESNIEGTS).count();
        long inReview = permits.stream().filter(p -> p.getStatus() == PermitStatus.IZSKATISANA).count();
        long approved = permits.stream().filter(p -> p.getStatus() == PermitStatus.APSTIPRINATS).count();
        long rejected = permits.stream().filter(p -> p.getStatus() == PermitStatus.NORAIDITS).count();
        model.addAttribute("countSubmitted", submitted);
        model.addAttribute("countInReview", inReview);
        model.addAttribute("countApproved", approved);
        model.addAttribute("countRejected", rejected);
        return "dashboard";
    }
 
    @GetMapping("/permits/new")
    public String newPermitPage(Model model) {
        return "permits/new";
    }
 
    @PostMapping("/permits/new")
    public String submitPermit(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam("title") String title,
                           @RequestParam("description") String description,
                           @RequestParam("tradeLocation") String tradeLocation,
                           @RequestParam("tradeStartDate") String tradeStartDate,
                           @RequestParam("tradeEndDate") String tradeEndDate,
                           @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
                               RedirectAttributes redirectAttributes) {
        try {
            MyUser user = userService.findByUsername(userDetails.getUsername());
            permitService.createPermit(title, description, tradeLocation,
                    tradeStartDate, tradeEndDate, documents, user);
            redirectAttributes.addFlashAttribute("success", "Pieteikums veiksmīgi iesniegts!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Kļūda: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
 
    @GetMapping("/permits/{id}")
    public String viewPermit(@PathVariable("id") long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        Permit permit = permitService.getPermitById(id);
        MyUser user = userService.findByUsername(userDetails.getUsername());
 
        // Check ownership or admin
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && permit.getUser().getUId() != user.getUId()) {
            return "redirect:/dashboard";
        }
 
        model.addAttribute("permit", permit);
        model.addAttribute("statuses", PermitStatus.values());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("documentList", permit.getDocumentList());
        return "permits/view";
    }

    @GetMapping("/permits/{id}/download/{storedName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") long id,
                                                 @PathVariable("storedName") String storedName,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        Permit permit = permitService.getPermitById(id);
        MyUser user = userService.findByUsername(userDetails.getUsername());
 
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && permit.getUser().getUId() != user.getUId()) {
            return ResponseEntity.status(403).build();
        }
 
        // Find original name for this stored file
        String originalName = storedName;
        for (String[] doc : permit.getDocumentList()) {
            if (doc[1].equals(storedName)) {
                originalName = doc[0];
                break;
            }
        }
 
        try {
            Path filePath = permitService.getFilePath(storedName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
 
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + originalName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}