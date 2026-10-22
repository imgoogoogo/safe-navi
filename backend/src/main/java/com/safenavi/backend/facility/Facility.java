package com.safenavi.backend.facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Facility")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One of {@link FacilityType} codes. */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false)
    private Boolean usedForRouting = true;

    /** Source-dataset identifier; unique so repeated syncs upsert instead of duplicating. */
    @Column(unique = true, length = 100)
    private String externalId;

    @Column(nullable = false, length = 30)
    private String sourceDataType;

    @Column(nullable = false, length = 10)
    private String regionCode;

    protected Facility() {
    }

    public Facility(String type, Double lat, Double lng, String externalId, String sourceDataType, String regionCode) {
        this.type = type;
        this.lat = lat;
        this.lng = lng;
        this.externalId = externalId;
        this.sourceDataType = sourceDataType;
        this.regionCode = regionCode;
    }

    public Long getId() {
        return id;
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

    public Boolean getUsedForRouting() {
        return usedForRouting;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getSourceDataType() {
        return sourceDataType;
    }

    public String getRegionCode() {
        return regionCode;
    }
}
