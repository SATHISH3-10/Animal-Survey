package com.example.vilage_wise.models;

import java.io.Serializable;

public class AnimalSurvey implements Serializable {
    private String id;
    private String villageId;
    private String villageName;
    private String ownerId;
    private String ownerName;
    private String animalCategory; // "Pets", "Livestock", "Poultry", "Other"
    private String animalType;     // "Cows", "Buffaloes", "Goats", "Sheep", "Pigs", "Dogs", "Cats", "Pet Birds", "Chickens", "Ducks", "Other"
    private String customAnimalType; // used when animalType is "Other"
    private int quantity;
    
    // Location Fields
    private String locality;
    private String pincode;
    private String district;
    private String state;
    private String country;
    private double latitude;
    private double longitude;
    private String locationSource; // "GPS" or "MANUAL"

    // Optional details
    private String breed;
    private String age;
    private String gender;
    private String notes;
    
    // Officer Attribution
    private String surveyorId;
    private String surveyorName;
    private String surveyorEmail;

    // Dates/Timestamps
    private long surveyDate;
    private long createdAt;
    private long updatedAt;

    public AnimalSurvey() {
        // Required empty constructor for Firestore
        this.country = "India";
        this.locationSource = "MANUAL";
    }

    public AnimalSurvey(String id, String villageId, String villageName, String ownerId, String ownerName, 
                        String animalCategory, String animalType, String customAnimalType, int quantity, 
                        String breed, String age, String gender, String notes, 
                        long surveyDate, long createdAt, long updatedAt) {
        this.id = id;
        this.villageId = villageId;
        this.villageName = villageName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.animalCategory = animalCategory;
        this.animalType = animalType;
        this.customAnimalType = customAnimalType;
        this.quantity = quantity;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.notes = notes;
        this.surveyDate = surveyDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getOwnerId() {
        return ownerId != null ? ownerId : "";
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName != null ? ownerName : "";
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getAnimalCategory() {
        return animalCategory != null ? animalCategory : "";
    }

    public void setAnimalCategory(String animalCategory) {
        this.animalCategory = animalCategory;
    }

    public String getAnimalType() {
        return animalType != null ? animalType : "";
    }

    public void setAnimalType(String animalType) {
        this.animalType = animalType;
    }

    public String getCustomAnimalType() {
        return customAnimalType != null ? customAnimalType : "";
    }

    public void setCustomAnimalType(String customAnimalType) {
        this.customAnimalType = customAnimalType;
    }

    public String getDisplayAnimalType() {
        if ("Other".equalsIgnoreCase(animalType) && customAnimalType != null && !customAnimalType.trim().isEmpty()) {
            return customAnimalType.trim();
        }
        return animalType != null ? animalType : "";
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getBreed() {
        return breed != null ? breed : "";
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getAge() {
        return age != null ? age : "";
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender != null ? gender : "";
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getSurveyDate() {
        return surveyDate;
    }

    public void setSurveyDate(long surveyDate) {
        this.surveyDate = surveyDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSurveyorId() {
        return surveyorId != null ? surveyorId : "";
    }

    public void setSurveyorId(String surveyorId) {
        this.surveyorId = surveyorId;
    }

    public String getSurveyorName() {
        return surveyorName != null ? surveyorName : "";
    }

    public void setSurveyorName(String surveyorName) {
        this.surveyorName = surveyorName;
    }

    public String getSurveyorEmail() {
        return surveyorEmail != null ? surveyorEmail : "";
    }

    public void setSurveyorEmail(String surveyorEmail) {
        this.surveyorEmail = surveyorEmail;
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
}
