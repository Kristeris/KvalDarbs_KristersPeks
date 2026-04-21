package lv.example.MarketPermitSystem.model;
 
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lv.example.MarketPermitSystem.model.enums.PermitStatus;
 
@Table(name = "PermitTable")
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Permit {
 
    @Column(name = "PId")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(value = AccessLevel.NONE)
    private long pId;
 
    @NotNull
    @Size(min = 3, max = 100)
    @Column(name = "Title")
    private String title;
 
    @Size(max = 500)
    @Column(name = "Description")
    private String description;
 
    @Column(name = "TradeLocation")
    private String tradeLocation;
 
    @Column(name = "TradeStartDate")
    private String tradeStartDate;
 
    @Column(name = "TradeEndDate")
    private String tradeEndDate;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    private PermitStatus status = PermitStatus.IESNIEGTS;
 
    @Column(name = "SubmittedAt")
    private LocalDateTime submittedAt;
 
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
 
    @Column(name = "AdminComment")
    private String adminComment;
 
    @Column(name = "DocumentFileName")
    private String documentFileName;
 
    @Column(name = "DocumentFilePath")
    private String documentFilePath;
 
    @ManyToOne
    @JoinColumn(name = "UId")
    private MyUser user;
 
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
 
    public Permit(String title, String description, String tradeLocation,
                  String tradeStartDate, String tradeEndDate, MyUser user) {
        this.title = title;
        this.description = description;
        this.tradeLocation = tradeLocation;
        this.tradeStartDate = tradeStartDate;
        this.tradeEndDate = tradeEndDate;
        this.user = user;
    }
}