package lv.example.MarketPermitSystem.service;
 
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.repo.MyAuthorityRepository;
import lv.example.MarketPermitSystem.repo.MyUserRepository;
 
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

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
 
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Autentifikācijas mēģinājums: '{}'", username);
        MyUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Lietotājs nav atrasts: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getAuthority().getTitle()))
        );
    }
 
    public MyUser registerUser(String username, String rawPassword, String email) {
        log.info("Reģistrācijas mēģinājums: '{}'", username);
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
        log.info("Lietotājs '{}' veiksmīgi reģistrēts", username);
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