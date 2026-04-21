package lv.example.MarketPermitSystem.config;
 
import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.repo.MyAuthorityRepository;
import lv.example.MarketPermitSystem.repo.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
 
@Component
public class DataInitializer implements CommandLineRunner {
 
    @Autowired
    private MyAuthorityRepository authorityRepository;
 
    @Autowired
    private MyUserRepository userRepository;
 
    @Autowired
    private PasswordEncoder passwordEncoder;
 
    @Override
    public void run(String... args) {
        // Create roles if not exist
        MyAuthority adminRole = authorityRepository.findByTitle("ADMIN")
                .orElseGet(() -> authorityRepository.save(new MyAuthority("ADMIN")));
        authorityRepository.findByTitle("USER")
                .orElseGet(() -> authorityRepository.save(new MyAuthority("USER")));
 
        // Create default admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            MyUser admin = new MyUser("admin", passwordEncoder.encode("admin123"),
                    "admin@ventspils.lv", adminRole);
            userRepository.save(admin);
            System.out.println("Admins izveidots: admin / admin123");
        }
    }
}