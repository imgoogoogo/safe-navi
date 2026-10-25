package com.safenavi.backend.report;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "Report")
public class Report {

    public static final String STATUS_ACTIVE = "ACTIVE";
    /** Soft-deleted by an admin. */
    public static final String STATUS_REMOVED = "REMOVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    /** One of {@link ReportCodes#TYPES}. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    private String address;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 500)
    private String memo;

    /** One of {@link ReportCodes#REVIEW_STATUSES}. */
    @Column(nullable = false, length = 20)
    private String reviewStatus = ReportCodes.REVIEW_RECEIVED;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected Report() {
    }

    public Report(User user, String type, Double lat, Double lng, String address, String imageUrl,
            String thumbnailUrl, String memo) {
        this.user = user;
        this.type = type;
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.memo = memo;
    }

    public boolean isOwnedBy(Long userId) {
        return userId != null && user.getId().equals(userId);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public void changeType(String type) {
        this.type = type;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public void relocate(Double lat, Double lng, String address) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
    }

    public void replaceImage(String imageUrl, String thumbnailUrl) {
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void remove() {
        this.status = STATUS_REMOVED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public String getAddress() {
        return address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getMemo() {
        return memo;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
