package com.safenavi.backend.favorite;

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
@Table(name = "Favorite")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Favorite() {
    }

    public Favorite(User user, String label, Double lat, Double lng) {
        this.user = user;
        this.label = label;
        this.lat = lat;
        this.lng = lng;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getLabel() {
        return label;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
