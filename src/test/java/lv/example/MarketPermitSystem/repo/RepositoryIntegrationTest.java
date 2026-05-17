package lv.example.MarketPermitSystem.repo;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RepositoryIntegrationTest {
 
    @Autowired
    private PermitRepository permitRepository;
 
    @Autowired
    private MyUserRepository userRepository;
 
    @Autowired
    private MyAuthorityRepository authorityRepository;
 
    @Autowired
    private TestEntityManager entityManager;
 
    private MyUser user1;
    private MyUser user2;
 
    @BeforeEach
    void setUp() {
        permitRepository.deleteAll();
        userRepository.deleteAll();
        authorityRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
 
        MyAuthority role = entityManager.persistAndFlush(new MyAuthority("USER"));
        user1 = entityManager.persistAndFlush(new MyUser("janis", "$2a$hash", "janis@lv", role));
        user2 = entityManager.persistAndFlush(new MyUser("anna",  "$2a$hash", "anna@lv",  role));
    }
 
    // MyUserRepository testi
 
    @Test
    @DisplayName("findByUsername: atrod eksistējošu lietotāju")
    void findByUsername_existingUser_found() {
        Optional<MyUser> result = userRepository.findByUsername("janis");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("janis@lv");
    }
 
    @Test
    @DisplayName("findByUsername: neeksistējošs → Optional.empty")
    void findByUsername_nonExisting_empty() {
        assertThat(userRepository.findByUsername("neeksiste")).isEmpty();
    }
 
    @Test
    @DisplayName("existsByUsername: atgriež true ja lietotājs eksistē")
    void existsByUsername_existing_returnsTrue() {
        assertThat(userRepository.existsByUsername("janis")).isTrue();
        assertThat(userRepository.existsByUsername("neeksiste")).isFalse();
    }
 
    @Test
    @DisplayName("existsByEmail: atgriež true ja e-pasts eksistē")
    void existsByEmail_existing_returnsTrue() {
        assertThat(userRepository.existsByEmail("janis@lv")).isTrue();
        assertThat(userRepository.existsByEmail("neviens@lv")).isFalse();
    }
 
    @Test
    @DisplayName("findByEmail: atrod pēc e-pasta adreses")
    void findByEmail_existing_found() {
        Optional<MyUser> result = userRepository.findByEmail("anna@lv");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("anna");
    }
 
    // MyAuthorityRepository testi
 
    @Test
    @DisplayName("findByTitle: atrod eksistējošu lomu")
    void findByTitle_existing_found() {
        assertThat(authorityRepository.findByTitle("USER")).isPresent();
    }
 
    @Test
    @DisplayName("findByTitle: neeksistējoša loma → Optional.empty")
    void findByTitle_nonExisting_empty() {
        assertThat(authorityRepository.findByTitle("SUPERADMIN")).isEmpty();
    }
 
    // PermitRepository testi
 
    @Test
    @DisplayName("findByUserOrderBySubmittedAtDesc: atgriež tikai šī lietotāja pieteikumus")
    void findByUser_returnsOnlyUsersPermits() {
        persistPermit("Pieteikums A", user1, PermitStatus.IESNIEGTS);
        persistPermit("Pieteikums B", user1, PermitStatus.APSTIPRINATS);
        persistPermit("Pieteikums C", user2, PermitStatus.IESNIEGTS);
 
        List<Permit> permits = permitRepository.findByUserOrderBySubmittedAtDesc(user1);
 
        assertThat(permits).hasSize(2);
        assertThat(permits).allMatch(p -> p.getUser().getUsername().equals("janis"));
    }
 
    @Test
    @DisplayName("findByUserOrderBySubmittedAtDesc (paginācija): atgriež lapu")
    void findByUserPaged_returnsPage() {
        for (int i = 0; i < 15; i++) {
            persistPermit("Pieteikums " + i, user1, PermitStatus.IESNIEGTS);
        }
 
        Page<Permit> page = permitRepository.findByUserOrderBySubmittedAtDesc(
                user1, PageRequest.of(0, 10));
 
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
 
    @Test
    @DisplayName("findAllByOrderBySubmittedAtDesc: atgriež visus pieteikumus")
    void findAll_returnsAllPermits() {
        persistPermit("P1", user1, PermitStatus.IESNIEGTS);
        persistPermit("P2", user2, PermitStatus.APSTIPRINATS);
 
        assertThat(permitRepository.findAllByOrderBySubmittedAtDesc()).hasSize(2);
    }
 
    @Test
    @DisplayName("findByStatus: atgriež tikai norādītā statusa pieteikumus")
    void findByStatus_returnsMatchingPermits() {
        persistPermit("Iesn.",     user1, PermitStatus.IESNIEGTS);
        persistPermit("Apst.",     user1, PermitStatus.APSTIPRINATS);
        persistPermit("Noraidīts", user2, PermitStatus.NORAIDITS);
 
        List<Permit> iesniegti = permitRepository.findByStatus(PermitStatus.IESNIEGTS);
 
        assertThat(iesniegti).hasSize(1);
        assertThat(iesniegti.get(0).getTitle()).isEqualTo("Iesn.");
    }
 
    @Test
    @DisplayName("countByStatus: korekti skaita pieteikumus pēc statusa")
    void countByStatus_returnsCorrectCount() {
        persistPermit("P1", user1, PermitStatus.IESNIEGTS);
        persistPermit("P2", user1, PermitStatus.IESNIEGTS);
        persistPermit("P3", user2, PermitStatus.APSTIPRINATS);
 
        assertThat(permitRepository.countByStatus(PermitStatus.IESNIEGTS)).isEqualTo(2);
        assertThat(permitRepository.countByStatus(PermitStatus.APSTIPRINATS)).isEqualTo(1);
        assertThat(permitRepository.countByStatus(PermitStatus.NORAIDITS)).isEqualTo(0);
    }
 
    @Test
    @DisplayName("findByUserUsernameContainingIgnoreCase: meklēšana pēc daļēja lietotājvārda")
    void searchByUsername_partialMatch_returnsResults() {
        persistPermit("P1", user1, PermitStatus.IESNIEGTS);
        persistPermit("P2", user2, PermitStatus.IESNIEGTS);
 
        Page<Permit> result = permitRepository
                .findByUserUsernameContainingIgnoreCaseOrderBySubmittedAtDesc(
                    "jan", PageRequest.of(0, 10));
 
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUser().getUsername()).isEqualTo("janis");
    }
 
    @Test
    @DisplayName("findByUserUsernameContainingIgnoreCase: lielo/mazo burtu neatšķiršana")
    void searchByUsername_caseInsensitive_returnsResults() {
        persistPermit("P1", user1, PermitStatus.IESNIEGTS);
 
        Page<Permit> result = permitRepository
                .findByUserUsernameContainingIgnoreCaseOrderBySubmittedAtDesc(
                    "JANIS", PageRequest.of(0, 10));
 
        assertThat(result.getContent()).hasSize(1);
    }
 
    @Test
    @DisplayName("Pieteikuma saglabāšana: @PrePersist aizpilda submittedAt un updatedAt")
    void save_permit_setsTimestamps() {
        Permit saved = persistPermit("Test", user1, PermitStatus.IESNIEGTS);
        entityManager.refresh(saved);
 
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
 
    // saglabā un uzreiz flush uz DB
 
    private Permit persistPermit(String title, MyUser user, PermitStatus status) {
        Permit p = new Permit(title, "Apraksts", "Lielā iela 1",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(10).toString(), user);
        p.setStatus(status);
        return entityManager.persistAndFlush(p);
    }
}
