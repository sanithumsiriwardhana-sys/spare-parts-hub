package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Public gateway submission from a prospective vendor (Function 6 - External
 * Supplier Portal). Admin reviews these and, if approved, the supplier is
 * onboarded as a Supplier record.
 */
@Entity
@Table(name = "partnership_request")
public class PartnershipRequest {

    // Constant names are lowercase to match the underlying MySQL ENUM
    // literals exactly ('pending', 'approved', 'rejected').
    public enum Status {
        pending, approved, rejected
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "contact_person", nullable = false, length = 50)
    private String contactPerson;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "catalog_file_path", length = 255)
    private String catalogFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.pending;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    public PartnershipRequest() {
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCatalogFilePath() {
        return catalogFilePath;
    }

    public void setCatalogFilePath(String catalogFilePath) {
        this.catalogFilePath = catalogFilePath;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
