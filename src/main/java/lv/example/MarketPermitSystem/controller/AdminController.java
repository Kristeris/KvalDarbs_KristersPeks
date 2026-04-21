package lv.example.MarketPermitSystem.controller;
 
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.service.PermitService;
import lv.example.MarketPermitSystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import java.util.List;
 
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 
    @Autowired
    private PermitService permitService;
 
    @Autowired
    private UserService userService;
 
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<Permit> permits = permitService.getAllPermits();
        model.addAttribute("permits", permits);
        model.addAttribute("totalCount", permits.size());
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
                           RedirectAttributes redirectAttributes) {
        try {
            PermitStatus newStatus = PermitStatus.valueOf(status);
            permitService.updateStatus(id, newStatus, adminComment);
            redirectAttributes.addFlashAttribute("success", "Statuss veiksmīgi atjaunināts un lietotājs informēts!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Kļūda: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}