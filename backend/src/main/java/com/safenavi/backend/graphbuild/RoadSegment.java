package com.safenavi.backend.graphbuild;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Undirected edge between two RoadNodes. nodeA/nodeB are just the two ends; geometry is a JSON
 * array of [lat, lng] points ordered from nodeA to nodeB (see PolylineCodec).
 */
@Entity
@Table(name = "RoadSegment")
public class RoadSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nodeAId", nullable = false)
    private RoadNode nodeA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nodeBId", nullable = false)
    private RoadNode nodeB;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String geometry;

    @Column(nullable = false)
    private Double distanceMeters;

    @Column(nullable = false, length = 10)
    private String regionCode;

    protected RoadSegment() {
    }

    public RoadSegment(RoadNode nodeA, RoadNode nodeB, String geometry, Double distanceMeters, String regionCode) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.geometry = geometry;
        this.distanceMeters = distanceMeters;
        this.regionCode = regionCode;
    }

    public Long getId() {
        return id;
    }

    public RoadNode getNodeA() {
        return nodeA;
    }

    public RoadNode getNodeB() {
        return nodeB;
    }

    public String getGeometry() {
        return geometry;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public String getRegionCode() {
        return regionCode;
    }
}
