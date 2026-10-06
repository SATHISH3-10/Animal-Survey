package com.example.vilage_wise.models;

import java.io.Serializable;

public class Village implements Serializable {
    private String id;
    private String name;
    private String locality;
    private String pincode;
    private String district;
    private String state;
    private String country;
    private String code; // optional
    private double latitude;
    private double longitude;
    private String locationSource; // "GPS" or "MANUAL"
    private long createdAt;

    public Village() {
        // Required empty constructor for Firestore
        this.country = "India";
        this.locationSource = "MANUAL";
    }

    public Village(String id, String name, String district, String state, String code, long createdAt) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.state = state;
        this.code = code;
        this.country = "India";
        this.locationSource = "MANUAL";
        this.createdAt = createdAt;
    }

    public Village(String id, String name, String locality, String pincode, String district, 
                   String state, String country, String code, double latitude, double longitude, 
                   String locationSource, long createdAt) {
        this.id = id;
        this.name = name;
        this.locality = locality;
        this.pincode = pincode;
        this.district = district;
        this.state = state;
        this.country = (country != null && !country.trim().isEmpty()) ? country : "India";
        this.code = code;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationSource = (locationSource != null && !locationSource.trim().isEmpty()) ? locationSource : "MANUAL";
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocality() {
        return locality != null ? locality : "";
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPincode() {
        return pincode != null ? pincode : "";
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getDistrict() {
        return district != null ? district : "";
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state != null ? state : "";
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country != null && !country.trim().isEmpty() ? country : "India";
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLocationSource() {
        return locationSource != null && !locationSource.trim().isEmpty() ? locationSource : "MANUAL";
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        if (district != null && !district.trim().isEmpty()) {
            return name + " (" + district + ")";
        }
        return name != null ? name : "";
    }
}

