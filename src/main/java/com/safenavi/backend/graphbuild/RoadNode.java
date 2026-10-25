package com.safenavi.backend.graphbuild;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "RoadNode")
public class RoadNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false, length = 10)
    private String regionCode;

    protected RoadNode() {
    }

    public RoadNode(Double lat, Double lng, String regionCode) {
        this.lat = lat;
        this.lng = lng;
        this.regionCode = regionCode;
    }

    public Long getId() {
        return id;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public String getRegionCode() {
        return regionCode;
    }
}
