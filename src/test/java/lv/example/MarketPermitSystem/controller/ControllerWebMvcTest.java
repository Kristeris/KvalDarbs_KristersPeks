package lv.example.MarketPermitSystem.controller;
 
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl; // FIX: Spring Boot 4.0 aizstāj @MockBean
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import lv.example.MarketPermitSystem.model.MyAuthority;
import lv.example.MarketPermitSystem.model.MyUser;
import lv.example.MarketPermitSystem.model.Permit;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import lv.example.MarketPermitSystem.service.PermitService;
import lv.example.MarketPermitSystem.service.UserService;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(controllers = {PermitController.class, AuthController.class, AdminController.class})
class ControllerWebMvcTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @MockitoBean
    private PermitService permitService;
 
    @MockitoBean
    private UserService userService;
 
    private MyUser testUser;
    private Permit testPermit;
 
    @BeforeEach
    void setUp() {
        MyAuthority role = new MyAuthority("USER");
        testUser = new MyUser("janis", "$2a$hash", "janis@lv", role);
 
        testPermit = new Permit("Tirdzniecība Lielā ielā", "Apraksts",
                "Lielā iela 5", LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(10).toString(), testUser);
        testPermit.setStatus(PermitStatus.IESNIEGTS);
    }
 
    // Publiskie maršruti - bez autentifikācijas
 
    @Test
    @DisplayName("GET /login → 200 OK (publiski pieejams)")
    void loginPage_publicAccess_returns200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }
 
    @Test
    @DisplayName("GET /register → 200 OK (publiski pieejams)")
    void registerPage_publicAccess_returns200() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }
 
    @Test
    @DisplayName("GET /login ar error parametru → kļūdas ziņojums modelī")
    void loginPage_withErrorParam_addsErrorToModel() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }
 
    @Test
    @DisplayName("GET /login ar logout parametru → ziņojums modelī")
    void loginPage_withLogoutParam_addsMessageToModel() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("message"));
    }
 
    // Aizsargātie maršruti - novirzīšana bez auth
 
    @Test
    @DisplayName("GET /dashboard bez autentifikācijas → novirzīšana uz login")
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
 
    @Test
    @DisplayName("GET /admin/dashboard bez autentifikācijas → novirzīšana")
    void adminDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
 
    // Dashboard - autentificēts lietotājs
 
    @Test
    @WithMockUser(username = "janis", roles = "USER")
    @DisplayName("GET /dashboard ar autentifikāciju → 200 OK")
    void dashboard_authenticated_returns200() throws Exception {
        when(userService.findByUsername("janis")).thenReturn(testUser);
        Page<Permit> emptyPage = new PageImpl<>(List.of());
        when(permitService.getUserPermitsPaged(any(), eq(0))).thenReturn(emptyPage);
        when(permitService.getUserPermits(any())).thenReturn(List.of());
 
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user", "permits"));
    }
 
    @Test
    @WithMockUser(username = "janis", roles = "USER")
    @DisplayName("GET /permits/new → 200 OK")
    void newPermitPage_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/permits/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("permits/new"));
    }
 
    // POST /register
 
    @Test
    @DisplayName("POST /register: paroles nesakrīt → novirzīšana ar kļūdu")
    void register_passwordMismatch_redirectsWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "jaunslietotajs")
                        .param("password", "parole1")
                        .param("confirmPassword", "parole2")
                        .param("email", "jauns@lv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
 
    @Test
    @DisplayName("POST /register: reģistrācija veiksmīga → novirzīšana uz login")
    void register_success_redirectsToLogin() throws Exception {
        when(userService.registerUser(eq("jaunslietotajs"), eq("parole123"), eq("jauns@lv")))
                .thenReturn(testUser);
 
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "jaunslietotajs")
                        .param("password", "parole123")
                        .param("confirmPassword", "parole123")
                        .param("email", "jauns@lv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }
 
    @Test
    @DisplayName("POST /register: lietotājvārds aizņemts → novirzīšana ar kļūdu")
    void register_usernameExists_redirectsWithError() throws Exception {
        when(userService.registerUser(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Lietotājvārds jau aizņemts"));
 
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "janis")
                        .param("password", "parole123")
                        .param("confirmPassword", "parole123")
                        .param("email", "jauns@lv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
 
    // Pieteikuma skatīšana
 
    @Test
    @WithMockUser(username = "janis", roles = "USER")
    @DisplayName("GET /permits/{id}: pieteikuma īpašnieks → 200 OK")
    void viewPermit_owner_returns200() throws Exception {
        when(permitService.getPermitById(1L)).thenReturn(testPermit);
        when(userService.findByUsername("janis")).thenReturn(testUser);
 
        mockMvc.perform(get("/permits/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("permits/view"))
                .andExpect(model().attributeExists("permit"));
    }
 
    // Admin dashboard
 
    @Test
    @WithMockUser(username = "admins", roles = "ADMIN")
    @DisplayName("GET /admin/dashboard ar ADMIN lomu → 200 OK")
    void adminDashboard_adminRole_returns200() throws Exception {
        Page<Permit> emptyPage = new PageImpl<>(List.of());
        when(permitService.getAllPermitsPaged(eq(0), any())).thenReturn(emptyPage);
        when(permitService.getAllPermits()).thenReturn(List.of());
        when(permitService.countByStatus(any())).thenReturn(0L);
        when(userService.findAll()).thenReturn(List.of());
 
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
 
    @Test
    @WithMockUser(username = "janis", roles = "USER")
    @DisplayName("GET /admin/dashboard ar USER lomu → 403 Forbidden")
    void adminDashboard_userRole_returns403() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
 
    // Admin statusa maiņa
 
    @Test
    @WithMockUser(username = "admins", roles = "ADMIN")
    @DisplayName("POST /admin/permits/{id}/status → statuss mainīts, novirzīšana")
    void updateStatus_admin_redirectsToDashboard() throws Exception {
        when(permitService.updateStatus(eq(1L), eq(PermitStatus.APSTIPRINATS), any()))
                .thenReturn(testPermit);
 
        mockMvc.perform(post("/admin/permits/1/status")
                        .with(csrf())
                        .param("status", "APSTIPRINATS")
                        .param("adminComment", "Apstiprināts!")
                        .param("page", "0")
                        .param("search", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/dashboard*"))
                .andExpect(flash().attributeExists("success"));
    }
 
    @Test
    @WithMockUser(username = "admins", roles = "ADMIN")
    @DisplayName("POST /admin/permits/{id}/status ar kļūdu → flash kļūda")
    void updateStatus_serviceThrows_redirectsWithError() throws Exception {
        when(permitService.updateStatus(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("Pieteikums nav atrasts"));
 
        mockMvc.perform(post("/admin/permits/1/status")
                        .with(csrf())
                        .param("status", "APSTIPRINATS")
                        .param("page", "0")
                        .param("search", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
 
    // Pieteikuma dzēšana
 
    @Test
    @WithMockUser(username = "janis", roles = "USER")
    @DisplayName("POST /permits/{id}/delete: veiksmīga dzēšana → flash success")
    void deletePermit_success_redirectsWithSuccess() throws Exception {
        when(userService.findByUsername("janis")).thenReturn(testUser);
        doNothing().when(permitService).deletePermit(eq(1L), eq(testUser));
 
        mockMvc.perform(post("/permits/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("success"));
    }
 
    @Test
    @WithMockUser(username = "janis", roles = "USER")
    @DisplayName("POST /permits/{id}/delete: nav tiesību → flash error")
    void deletePermit_unauthorized_redirectsWithError() throws Exception {
        when(userService.findByUsername("janis")).thenReturn(testUser);
        doThrow(new IllegalArgumentException("Nav tiesību dzēst"))
                .when(permitService).deletePermit(eq(1L), eq(testUser));
 
        mockMvc.perform(post("/permits/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}
 