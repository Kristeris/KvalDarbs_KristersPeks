package lv.example.MarketPermitSystem.service;
 
import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.repo.MyAuthorityRepository;
import lv.example.MarketPermitSystem.repo.MyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
 
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
 
    @Mock
    private MyUserRepository userRepository;
 
    @Mock
    private MyAuthorityRepository authorityRepository;
 
    @Mock
    private PasswordEncoder passwordEncoder;
 
    @Mock
    private EmailService emailService;
 
    @InjectMocks
    private UserService userService;
 
    private MyAuthority userRole;
    private MyUser testUser;
 
    @BeforeEach
    void setUp() {
        userRole = new MyAuthority("USER");
        testUser = new MyUser("janis123", "$2a$hash", "janis@example.lv", userRole);
    }
 
    // loadUserByUsername testi
 
    @Test
    @DisplayName("loadUserByUsername: lietotājs eksistē → korekts UserDetails")
    void loadUserByUsername_userExists_returnsUserDetails() {
        when(userRepository.findByUsername("janis123")).thenReturn(Optional.of(testUser));
 
        UserDetails details = userService.loadUserByUsername("janis123");
 
        assertThat(details.getUsername()).isEqualTo("janis123");
        assertThat(details.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }
 
    @Test
    @DisplayName("loadUserByUsername: lietotājs neeksistē → UsernameNotFoundException")
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("neeksistejs")).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> userService.loadUserByUsername("neeksistejs"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
 
    @Test
    @DisplayName("loadUserByUsername: ADMIN loma → ROLE_ADMIN piešķirta")
    void loadUserByUsername_adminUser_hasAdminRole() {
        MyAuthority adminRole = new MyAuthority("ADMIN");
        MyUser admin = new MyUser("admins", "$2a$hash", "admin@example.lv", adminRole);
        when(userRepository.findByUsername("admins")).thenReturn(Optional.of(admin));
 
        UserDetails details = userService.loadUserByUsername("admins");
 
        assertThat(details.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
 
    // registerUser testi
 
    @Test
    @DisplayName("registerUser: lietotājvārds jau aizņemts → kļūda")
    void registerUser_usernameAlreadyExists_throwsException() {
        when(userRepository.existsByUsername("janis123")).thenReturn(true);
 
        assertThatThrownBy(() -> userService.registerUser("janis123", "parole123", "cits@example.lv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aizņemts");
 
        verify(userRepository, never()).save(any());
    }
 
    @Test
    @DisplayName("registerUser: e-pasts jau reģistrēts → kļūda")
    void registerUser_emailAlreadyExists_throwsException() {
        when(userRepository.existsByUsername("jauns")).thenReturn(false);
        when(userRepository.existsByEmail("janis@example.lv")).thenReturn(true);
 
        assertThatThrownBy(() -> userService.registerUser("jauns", "parole123", "janis@example.lv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-pasts");
 
        verify(userRepository, never()).save(any());
    }
 
    @Test
    @DisplayName("registerUser: korekti dati → lietotājs saglabāts, e-pasts nosūtīts")
    void registerUser_validData_savesUserAndSendsEmail() {
        when(userRepository.existsByUsername("jauns")).thenReturn(false);
        when(userRepository.existsByEmail("jauns@example.lv")).thenReturn(false);
        when(authorityRepository.findByTitle("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("parole123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(MyUser.class))).thenAnswer(inv -> inv.getArgument(0));
 
        MyUser result = userService.registerUser("jauns", "parole123", "jauns@example.lv");
 
        assertThat(result.getUsername()).isEqualTo("jauns");
        assertThat(result.getEmail()).isEqualTo("jauns@example.lv");
        assertThat(result.getPassword()).isEqualTo("$2a$encoded");
        verify(emailService).sendRegistrationEmail(any(MyUser.class));
    }
 
    @Test
    @DisplayName("registerUser: bez e-pasta → reģistrācija veiksmīga")
    void registerUser_noEmail_registersSuccessfully() {
        when(userRepository.existsByUsername("jauns")).thenReturn(false);
        when(authorityRepository.findByTitle("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("parole123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(MyUser.class))).thenAnswer(inv -> inv.getArgument(0));
 
        MyUser result = userService.registerUser("jauns", "parole123", null);
 
        assertThat(result).isNotNull();
        // E-pasts nav jāsūta ja nav e-pasta adreses
        verify(emailService).sendRegistrationEmail(any());
    }
 
    @Test
    @DisplayName("registerUser: USER loma netiek atrasta → jauna loma tiek izveidota")
    void registerUser_userRoleNotFound_createsNewRole() {
        when(userRepository.existsByUsername("jauns")).thenReturn(false);
        when(userRepository.existsByEmail("jauns@example.lv")).thenReturn(false);
        when(authorityRepository.findByTitle("USER")).thenReturn(Optional.empty());
        when(authorityRepository.save(any(MyAuthority.class))).thenReturn(userRole);
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");
        when(userRepository.save(any(MyUser.class))).thenAnswer(inv -> inv.getArgument(0));
 
        userService.registerUser("jauns", "parole123", "jauns@example.lv");
 
        verify(authorityRepository).save(any(MyAuthority.class));
    }
 
    // findByUsername testi
 
    @Test
    @DisplayName("findByUsername: lietotājs eksistē → atgriež MyUser")
    void findByUsername_userExists_returnsUser() {
        when(userRepository.findByUsername("janis123")).thenReturn(Optional.of(testUser));
 
        MyUser result = userService.findByUsername("janis123");
 
        assertThat(result.getUsername()).isEqualTo("janis123");
    }
 
    @Test
    @DisplayName("findByUsername: lietotājs neeksistē → UsernameNotFoundException")
    void findByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("neeksistejs")).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> userService.findByUsername("neeksistejs"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
 
    @Test
    @DisplayName("findAll: atgriež visu lietotāju sarakstu")
    void findAll_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
 
        List<MyUser> result = userService.findAll();
 
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("janis123");
    }
}
