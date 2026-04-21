package lv.example.MarketPermitSystem.model.enums;
 
public enum PermitStatus {
    IESNIEGTS("Iesniegts"),
    IZSKATISANA("Izskatīšanā"),
    PAPILDINAJUMI_NEPIECIESAMI("Papildinājumi nepieciešami"),
    APSTIPRINATS("Apstiprināts"),
    NORAIDITS("Noraidīts");
 
    private final String displayName;
 
    PermitStatus(String displayName) {
        this.displayName = displayName;
    }
 
    public String getDisplayName() {
        return displayName;
    }
}
 