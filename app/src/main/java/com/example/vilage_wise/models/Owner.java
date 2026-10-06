package com.example.vilage_wise.models;

import java.io.Serializable;

public class Owner implements Serializable {
    private String id;
    private String name;
    private String contactNumber; // optional
    private String villageId;
    private String villageName; // helper for display
    private String addressLocality; // optional
    private long createdAt;

    public Owner() {
        // Required empty constructor for Firestore
    }

    public Owner(String id, String name, String contactNumber, String villageId, String villageName, String addressLocality, long createdAt) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.villageId = villageId;
        this.villageName = villageName;
        this.addressLocality = addressLocality;
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

    public String getContactNumber() {
        return contactNumber != null ? contactNumber : "";
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getVillageId() {
        return villageId != null ? villageId : "";
    }

    public void setVillageId(String villageId) {
        this.villageId = villageId;
    }

    public String getVillageName() {
        return villageName != null ? villageName : "";
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public String getAddressLocality() {
        return addressLocality != null ? addressLocality : "";
    }

    public void setAddressLocality(String addressLocality) {
        this.addressLocality = addressLocality;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
