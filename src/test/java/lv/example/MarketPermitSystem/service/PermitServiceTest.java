package lv.example.MarketPermitSystem.service;
 
import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.repo.PermitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
 
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class PermitServiceTest {
 
    @Mock
    private PermitRepository permitRepository;
 
    @Mock
    private EmailService emailService;
 
    @InjectMocks
    private PermitService permitService;
 
    private MyUser testUser;
    private MyAuthority userRole;
 
    @BeforeEach
    void setUp() {
        // Iestatām failu augšupielādes direktoriju caur reflection
        ReflectionTestUtils.setField(permitService, "uploadDir", "uploads");
 
        userRole = new MyAuthority("USER");
        testUser = new MyUser("testlietotajs", "encodedPass", "test@example.lv", userRole);
    }
 
    // createPermit - validācijas testi
 
    @Test
    @DisplayName("createPermit: sākuma datums pagātnē → kļūda")
    void createPermit_startDateInPast_throwsException() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        String tomorrow  = LocalDate.now().plusDays(1).toString();
 
        assertThatThrownBy(() ->
            permitService.createPermit("Nosaukums", "Apraksts", "Lielā iela 5",
                yesterday, tomorrow, null, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sākuma datums");
    }
 
    @Test
    @DisplayName("createPermit: beigu datums pirms sākuma datuma → kļūda")
    void createPermit_endBeforeStart_throwsException() {
        String start = LocalDate.now().plusDays(5).toString();
        String end   = LocalDate.now().plusDays(1).toString();
 
        assertThatThrownBy(() ->
            permitService.createPermit("Nosaukums", "Apraksts", "Lielā iela 5",
                start, end, null, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("beigu datums");
    }
 
    @Test
    @DisplayName("createPermit: beigu datums pagātnē → kļūda")
    void createPermit_endDateInPast_throwsException() {
        String start = LocalDate.now().plusDays(1).toString();
        String end   = LocalDate.now().minusDays(1).toString();
 
        assertThatThrownBy(() ->
            permitService.createPermit("Nosaukums", "Apraksts", "Lielā iela 5",
                start, end, null, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("beigu datums");
    }
 
    @Test
    @DisplayName("createPermit: nepareizs datuma formāts → kļūda")
    void createPermit_invalidDateFormat_throwsException() {
        assertThatThrownBy(() ->
            permitService.createPermit("Nosaukums", "Apraksts", "Lielā iela 5",
                "31-12-2026", "2026-12-31", null, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("formāts");
    }
 
    @Test
    @DisplayName("createPermit: korekti dati → pieteikums saglabāts un e-pasts nosūtīts")
    void createPermit_validData_savesPermitAndSendsEmail() throws IOException {
        String start = LocalDate.now().plusDays(1).toString();
        String end   = LocalDate.now().plusDays(10).toString();
 
        Permit savedPermit = new Permit("Nosaukums", "Apraksts", "Lielā iela 5", start, end, testUser);
        when(permitRepository.save(any(Permit.class))).thenReturn(savedPermit);
 
        Permit result = permitService.createPermit("Nosaukums", "Apraksts", "Lielā iela 5",
                start, end, null, testUser);
 
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Nosaukums");
        assertThat(result.getStatus()).isEqualTo(PermitStatus.IESNIEGTS);
        verify(permitRepository).save(any(Permit.class));
        verify(emailService).sendPermitSubmittedEmail(eq(testUser), any(Permit.class));
    }
 
    @Test
    @DisplayName("createPermit: šodienas datums ir atļauts")
    void createPermit_todayAsStartDate_succeeds() throws IOException {
        String today = LocalDate.now().toString();
        String end   = LocalDate.now().plusDays(5).toString();
 
        Permit savedPermit = new Permit("Nosaukums", "Apraksts", "Vieta", today, end, testUser);
        when(permitRepository.save(any(Permit.class))).thenReturn(savedPermit);
 
        assertThatCode(() ->
            permitService.createPermit("Nosaukums", "Apraksts", "Vieta",
                today, end, null, testUser))
            .doesNotThrowAnyException();
    }
 
    // updateStatus testi
 
    @Test
    @DisplayName("updateStatus: statuss mainīts un e-pasts nosūtīts")
    void updateStatus_changesStatusAndSendsEmail() {
        Permit permit = new Permit("Test", "Apraksts", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), testUser);
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
        when(permitRepository.save(any(Permit.class))).thenReturn(permit);
 
        Permit result = permitService.updateStatus(1L, PermitStatus.IZSKATISANA, "Tiek izskatīts");
 
        assertThat(result.getStatus()).isEqualTo(PermitStatus.IZSKATISANA);
        assertThat(result.getAdminComment()).isEqualTo("Tiek izskatīts");
        verify(emailService).sendStatusChangedEmail(eq(testUser), any(Permit.class));
    }
 
    @Test
    @DisplayName("updateStatus: tukšs komentārs → komentārs netiek mainīts")
    void updateStatus_emptyComment_doesNotOverwriteComment() {
        Permit permit = new Permit("Test", "Apraksts", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), testUser);
        permit.setAdminComment("Iepriekšējais komentārs");
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
        when(permitRepository.save(any(Permit.class))).thenReturn(permit);
 
        permitService.updateStatus(1L, PermitStatus.APSTIPRINATS, "");
 
        // Tukšs komentārs — service to neraksta pāri
        assertThat(permit.getAdminComment()).isEqualTo("Iepriekšējais komentārs");
    }
 
    @Test
    @DisplayName("updateStatus: neeksistējošs pieteikums → RuntimeException")
    void updateStatus_permitNotFound_throwsException() {
        when(permitRepository.findById(999L)).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> permitService.updateStatus(999L, PermitStatus.APSTIPRINATS, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nav atrasts");
    }
 
    // deletePermit testi
 
    @Test
    @DisplayName("deletePermit: cita lietotāja pieteikums → kļūda")
    void deletePermit_wrongUser_throwsException() {
        MyUser otherUser = new MyUser("cits", "pass", otherRole());
        Permit permit = new Permit("Test", "", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), otherUser);
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
 
        assertThatThrownBy(() -> permitService.deletePermit(1L, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nav tiesību");
    }
 
    @Test
    @DisplayName("deletePermit: statuss nav IESNIEGTS → kļūda")
    void deletePermit_wrongStatus_throwsException() {
        Permit permit = new Permit("Test", "", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), testUser);
        permit.setStatus(PermitStatus.IZSKATISANA);
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
 
        assertThatThrownBy(() -> permitService.deletePermit(1L, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nevar dzēst");
    }
 
    @Test
    @DisplayName("deletePermit: korekts pieteikums → tiek dzēsts")
    void deletePermit_validPermit_deletesPermit() {
        Permit permit = new Permit("Test", "", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), testUser);
        // Status pēc noklusējuma ir IESNIEGTS
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
 
        permitService.deletePermit(1L, testUser);
 
        verify(permitRepository).delete(permit);
    }
 
    // updatePermit testi
 
    @Test
    @DisplayName("updatePermit: pieteikums IZSKATISANA statusā → rediģēšana nav atļauta")
    void updatePermit_inReviewStatus_throwsException() {
        Permit permit = new Permit("Test", "", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), testUser);
        permit.setStatus(PermitStatus.IZSKATISANA);
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
 
        assertThatThrownBy(() -> permitService.updatePermit(1L, "Jauns", "", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(),
                null, null, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nevar rediģēt");
    }
 
    @Test
    @DisplayName("updatePermit: PAPILDINAJUMI_NEPIECIESAMI → pēc saglabāšanas statuss kļūst IESNIEGTS")
    void updatePermit_needsAdditions_resetsToSubmitted() throws IOException {
        Permit permit = new Permit("Test", "", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(), testUser);
        permit.setStatus(PermitStatus.PAPILDINAJUMI_NEPIECIESAMI);
        when(permitRepository.findById(1L)).thenReturn(Optional.of(permit));
        when(permitRepository.save(any(Permit.class))).thenAnswer(inv -> inv.getArgument(0));
 
        Permit result = permitService.updatePermit(1L, "Jauns nosaukums", "Apraksts", "Vieta",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(5).toString(),
                null, null, testUser);
 
        assertThat(result.getStatus()).isEqualTo(PermitStatus.IESNIEGTS);
    }
 
    @Test
    @DisplayName("countByStatus: atgriež repozitorija skaitli")
    void countByStatus_returnsRepositoryCount() {
        when(permitRepository.countByStatus(PermitStatus.APSTIPRINATS)).thenReturn(7L);
        assertThat(permitService.countByStatus(PermitStatus.APSTIPRINATS)).isEqualTo(7L);
    }
 
    @Test
    @DisplayName("getUserPermitsPaged: atgriež lapu ar pieteikumiem")
    void getUserPermitsPaged_returnsPage() {
        Page<Permit> mockPage = new PageImpl<>(List.of());
        when(permitRepository.findByUserOrderBySubmittedAtDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(mockPage);
 
        Page<Permit> result = permitService.getUserPermitsPaged(testUser, 0);
        assertThat(result).isNotNull();
    }
 
    private MyAuthority otherRole() {
        return new MyAuthority("USER");
    }
}