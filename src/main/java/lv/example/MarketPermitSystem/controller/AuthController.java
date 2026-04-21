package lv.example.MarketPermitSystem.controller;
 
import lv.example.MarketPermitSystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
@Controller
public class AuthController {
 
    @Autowired
    private UserService userService;
 
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
 
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Nepareizs lietotājvārds vai parole.");
        if (logout != null) model.addAttribute("message", "Jūs esat veiksmīgi izrakstījies.");
        return "auth/login";
    }
 
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }
 
    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               @RequestParam(value = "email", required = false) String email,
                               RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Paroles nesakrīt!");
            return "redirect:/register";
        }
        try {
            userService.registerUser(username, password, email);
            redirectAttributes.addFlashAttribute("success", "Reģistrācija veiksmīga! Lūdzu, pieslēdzieties.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}