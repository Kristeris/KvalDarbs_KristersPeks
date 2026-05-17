package lv.example.MarketPermitSystem.model;
 
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermitModelTest {
 
    // getDocumentList testi
 
    @Test
    @DisplayName("getDocumentList: tukšs documentFiles → tukšs saraksts")
    void getDocumentList_empty_returnsEmptyList() {
        Permit permit = new Permit();
        permit.setDocumentFiles(null);
 
        assertThat(permit.getDocumentList()).isEmpty();
 
        permit.setDocumentFiles("");
        assertThat(permit.getDocumentList()).isEmpty();
    }

    @Test
    @DisplayName("getDocumentList: viens dokuments → viens ieraksts")
    void getDocumentList_singleDocument_returnsOneEntry() {
        Permit permit = new Permit();
        permit.setDocumentFiles("dokuments.pdf::uuid-1234_dokuments.pdf");
 
        List<String[]> docs = permit.getDocumentList();
 
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0)[0]).isEqualTo("dokuments.pdf");
        assertThat(docs.get(0)[1]).isEqualTo("uuid-1234_dokuments.pdf");
    }
 
    @Test
    @DisplayName("getDocumentList: vairāki dokumenti → vairāki ieraksti")
    void getDocumentList_multipleDocuments_returnsAllEntries() {
        Permit permit = new Permit();
        permit.setDocumentFiles(
                "a.pdf::uuid1_a.pdf,b.docx::uuid2_b.docx,c.jpg::uuid3_c.jpg");
 
        List<String[]> docs = permit.getDocumentList();
 
        assertThat(docs).hasSize(3);
        assertThat(docs.get(0)[0]).isEqualTo("a.pdf");
        assertThat(docs.get(1)[0]).isEqualTo("b.docx");
        assertThat(docs.get(2)[0]).isEqualTo("c.jpg");
    }
 
    @Test
    @DisplayName("getDocumentList: nepareizs formāts → tiek izlaists")
    void getDocumentList_malformedEntry_isSkipped() {
        Permit permit = new Permit();
        // Viens ieraksts bez "::" separatora
        permit.setDocumentFiles("labais.pdf::uuid1_labais.pdf,slikts_ieraksts");
 
        List<String[]> docs = permit.getDocumentList();
 
        // Tikai korekts ieraksts tiek atgriezts
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0)[0]).isEqualTo("labais.pdf");
    }
 
    // PermitStatus displayName testi
 
    @Test
    @DisplayName("PermitStatus: visi statusi ir definēti ar displayName")
    void permitStatus_allStatuses_haveDisplayName() {
        for (PermitStatus status : PermitStatus.values()) {
            assertThat(status.getDisplayName())
                    .as("Statusam %s jābūt displayName", status)
                    .isNotBlank();
        }
    }
 
    @Test
    @DisplayName("PermitStatus: korekti displayName vērtības")
    void permitStatus_displayNames_areCorrect() {
        assertThat(PermitStatus.IESNIEGTS.getDisplayName()).isEqualTo("Iesniegts");
        assertThat(PermitStatus.IZSKATISANA.getDisplayName()).isEqualTo("Izskatīšanā");
        assertThat(PermitStatus.PAPILDINAJUMI_NEPIECIESAMI.getDisplayName())
                .isEqualTo("Papildinājumi nepieciešami");
        assertThat(PermitStatus.APSTIPRINATS.getDisplayName()).isEqualTo("Apstiprināts");
        assertThat(PermitStatus.NORAIDITS.getDisplayName()).isEqualTo("Noraidīts");
    }
 
    @Test
    @DisplayName("PermitStatus: ir tieši 5 statusi")
    void permitStatus_hasExactlyFiveValues() {
        assertThat(PermitStatus.values()).hasSize(5);
    }
 
    @Test
    @DisplayName("PermitStatus: valueOf darbojas korekti")
    void permitStatus_valueOf_worksCorrectly() {
        assertThat(PermitStatus.valueOf("IESNIEGTS")).isEqualTo(PermitStatus.IESNIEGTS);
        assertThat(PermitStatus.valueOf("APSTIPRINATS")).isEqualTo(PermitStatus.APSTIPRINATS);
    }
 
    // Permit konstruktors testi
 
    @Test
    @DisplayName("Permit konstruktors: noklusētais statuss ir IESNIEGTS")
    void permit_defaultStatus_isIesniegts() {
        MyAuthority role = new MyAuthority("USER");
        MyUser user = new MyUser("janis", "pass", role);
        Permit permit = new Permit("Nosaukums", "Apraksts", "Vieta",
                "2026-06-01", "2026-06-30", user);
 
        assertThat(permit.getStatus()).isEqualTo(PermitStatus.IESNIEGTS);
    }
 
    @Test
    @DisplayName("Permit: setter un getter darbojas korekti")
    void permit_settersAndGetters_workCorrectly() {
        Permit permit = new Permit();
        permit.setTitle("Mans pieteikums");
        permit.setDescription("Apraksts");
        permit.setTradeLocation("Lielā iela 5");
        permit.setAdminComment("Komentārs");
 
        assertThat(permit.getTitle()).isEqualTo("Mans pieteikums");
        assertThat(permit.getDescription()).isEqualTo("Apraksts");
        assertThat(permit.getTradeLocation()).isEqualTo("Lielā iela 5");
        assertThat(permit.getAdminComment()).isEqualTo("Komentārs");
    }
}
