package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "rma_claim")
public class RmaClaim {

    public enum Resolution {
        sent_to_manufacturer, refunded, replaced, pending
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    private Integer claimId;

    @Column(name = "claim_code", length = 20)
    private String claimCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_id", nullable = false)
    private SerialNumber serial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_by", nullable = false)
    private User processedBy;

    // UC-04 step 5: coordinator records the reported fault and the
    // item's physical condition before choosing a resolution.
    @Column(name = "fault_description", length = 255)
    private String faultDescription;

    @Column(name = "condition_notes", length = 255)
    private String conditionNotes;

    // NOT insertable=false here: schema defaults to CURRENT_DATE but the
    // app will typically want to set this explicitly at claim creation time.
    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", nullable = false)
    private Resolution resolution = Resolution.pending;

    public RmaClaim() {
    }

    public Integer getClaimId() {
        return claimId;
    }

    public void setClaimId(Integer claimId) {
        this.claimId = claimId;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public void setClaimCode(String claimCode) {
        this.claimCode = claimCode;
    }

    public SerialNumber getSerial() {
        return serial;
    }

    public void setSerial(SerialNumber serial) {
        this.serial = serial;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public String getFaultDescription() {
        return faultDescription;
    }

    public void setFaultDescription(String faultDescription) {
        this.faultDescription = faultDescription;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }
}
