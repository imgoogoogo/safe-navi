package com.safenavi.backend.report;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.safenavi.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ReportFlag")
public class ReportFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportId", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporterId", nullable = false)
    private User reporter;

    @Column(nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReportFlag() {
    }

    public ReportFlag(Report report, User reporter, String reason) {
        this.report = report;
        this.reporter = reporter;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Report getReport() {
        return report;
    }

    public User getReporter() {
        return reporter;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
