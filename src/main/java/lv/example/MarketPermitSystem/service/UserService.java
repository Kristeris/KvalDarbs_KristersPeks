package lv.example.MarketPermitSystem.service;
 
import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.repo.MyAuthorityRepository;
import lv.example.MarketPermitSystem.repo.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
 
import java.util.List;
 
@Service
public class UserService implements UserDetailsService {
 
    @Autowired
    private MyUserRepository userRepository;
 
    @Autowired
    private MyAuthorityRepository authorityRepository;
 
    @Autowired
    private PasswordEncoder passwordEncoder;
 
    @Autowired
    private EmailService emailService;
 
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MyUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Lietotājs nav atrasts: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getAuthority().getTitle()))
        );
    }
 
    public MyUser registerUser(String username, String rawPassword, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Lietotājvārds jau aizņemts");
        }
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-pasts jau reģistrēts");
        }
        MyAuthority userRole = authorityRepository.findByTitle("USER")
                .orElseGet(() -> authorityRepository.save(new MyAuthority("USER")));
        MyUser user = new MyUser(username, passwordEncoder.encode(rawPassword), email, userRole);
        MyUser saved = userRepository.save(user);
        emailService.sendRegistrationEmail(saved);
        return saved;
    }
 
    public MyUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Lietotājs nav atrasts"));
    }
 
    public List<MyUser> findAll() {
        return userRepository.findAll();
    }
}