package com.example.vilage_wise.models;

import java.io.Serializable;

public class OwnerAnimalCount implements Serializable {
    private String ownerId;
    private String ownerName;
    private String villageId;
    private String villageName;
    private String animalType;
    private int totalCount;
    private int surveyRecordsCount;

    public OwnerAnimalCount() {
    }

    public OwnerAnimalCount(String ownerId, String ownerName, String villageId, String villageName, String animalType, int totalCount, int surveyRecordsCount) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.villageId = villageId;
        this.villageName = villageName;
        this.animalType = animalType;
        this.totalCount = totalCount;
        this.surveyRecordsCount = surveyRecordsCount;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName != null ? ownerName : "Unknown Owner";
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getVillageId() {
        return villageId;
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

    public String getAnimalType() {
        return animalType != null ? animalType : "";
    }

    public void setAnimalType(String animalType) {
        this.animalType = animalType;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSurveyRecordsCount() {
        return surveyRecordsCount;
    }

    public void setSurveyRecordsCount(int surveyRecordsCount) {
        this.surveyRecordsCount = surveyRecordsCount;
    }
}
