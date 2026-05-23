package lv.example.MarketPermitSystem.controller;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.service.EmailService;
import lv.example.MarketPermitSystem.service.PermitService;
import lv.example.MarketPermitSystem.service.UserService;

 
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 
    @Autowired
    private PermitService permitService;
 
    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;
    
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
 
    @GetMapping("/dashboard")
    public String adminDashboard(@RequestParam(value = "page", defaultValue = "0") int page,@RequestParam(value = "search", defaultValue = "") String search, Model model) {
        // Paginēts saraksts
        Page<Permit> permitPage = permitService.getAllPermitsPaged(page, search);
        model.addAttribute("permits", permitPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", permitPage.getTotalPages());
        model.addAttribute("totalElements", permitPage.getTotalElements());
        model.addAttribute("search", search);
 
        // Statistika — par VISIEM pieteikumiem
        model.addAttribute("totalCount", permitService.getAllPermits().size());
        model.addAttribute("submittedCount", permitService.countByStatus(PermitStatus.IESNIEGTS));
        model.addAttribute("reviewCount", permitService.countByStatus(PermitStatus.IZSKATISANA));
        model.addAttribute("approvedCount", permitService.countByStatus(PermitStatus.APSTIPRINATS));
        model.addAttribute("rejectedCount", permitService.countByStatus(PermitStatus.NORAIDITS));
        model.addAttribute("users", userService.findAll());
        return "admin/dashboard";
    }
 
    @PostMapping("/permits/{id}/status")
    public String updateStatus(@PathVariable("id") long id,
                           @RequestParam("status") String status,
                           @RequestParam(value = "adminComment", required = false) String adminComment,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "search", defaultValue = "") String search,
                           RedirectAttributes redirectAttributes) {
        log.info("Admin maina pieteikuma #{} statusu uz '{}', komentārs: '{}'",
            id, status, adminComment);
        try {
            PermitStatus newStatus = PermitStatus.valueOf(status);
            permitService.updateStatus(id, newStatus, adminComment);
            redirectAttributes.addFlashAttribute("success", "Statuss veiksmīgi atjaunināts un lietotājs informēts!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Kļūda: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?page=" + page + "&search=" + search;
    }
    
    @PostMapping("/permits/{id}/send-edoc")
    public String sendEdoc(
            @PathVariable("id") long id,
            @RequestParam("edocFile") MultipartFile edocFile,
            RedirectAttributes redirectAttributes) {
 
        log.info("Admin augšupielādē eDoc pieteikumam #{}", id);
 
        // Pārbaude — fails nav tukšs
        if (edocFile == null || edocFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("edocError", "Lūdzu, izvēlieties eDoc failu!");
            return "redirect:/permits/" + id;
        }
 
        // Pārbaude — tikai .edoc, .pdf, .adoc paplašinājumi
        String filename = edocFile.getOriginalFilename() != null
                ? edocFile.getOriginalFilename().toLowerCase()
                : "";
        if (!filename.endsWith(".edoc") && !filename.endsWith(".pdf")
                && !filename.endsWith(".adoc") && !filename.endsWith(".asice")) {
            redirectAttributes.addFlashAttribute("edocError",
                "Atļautie failu formāti: .edoc, .asice, .adoc, .pdf");
            return "redirect:/permits/" + id;
        }
 
        try {
            Permit permit = permitService.getPermitById(id);
 
            // Pārbaude — pieteikumam jābūt apstiprinātam
            if (permit.getStatus() != PermitStatus.APSTIPRINATS) {
                redirectAttributes.addFlashAttribute("edocError",
                    "eDoc var nosūtīt tikai apstiprinātiem pieteikumiem!");
                return "redirect:/permits/" + id;
            }
 
            emailService.sendEdocEmail(permit.getUser(), permit, edocFile);
            log.info("eDoc veiksmīgi nosūtīts lietotājam '{}' par pieteikumu #{}",
                    permit.getUser().getUsername(), id);
            redirectAttributes.addFlashAttribute("edocSuccess",
                "eDoc veiksmīgi nosūtīts uz " + permit.getUser().getEmail() + "!");
 
        } catch (IllegalStateException e) {
            // Lietotājam nav e-pasta
            redirectAttributes.addFlashAttribute("edocError", e.getMessage());
        } catch (Exception e) {
            log.error("eDoc sūtīšana neizdevās pieteikumam #{}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("edocError",
                "Kļūda nosūtot eDoc: " + e.getMessage());
        }
 
        return "redirect:/permits/" + id;
    }
}