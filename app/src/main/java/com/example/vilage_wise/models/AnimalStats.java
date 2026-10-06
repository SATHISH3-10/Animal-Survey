package com.example.vilage_wise.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnimalStats implements Serializable {
    private int totalAnimals = 0;
    private int totalOwners = 0;
    private int totalSurveys = 0;
    private int totalLivestock = 0;
    private int totalPets = 0;
    private int totalPoultry = 0;
    private int totalOther = 0;
    
    // Animal counts
    private int cows = 0;
    private int buffaloes = 0;
    private int goats = 0;
    private int sheep = 0;
    private int dogs = 0;
    private int cats = 0;
    private int poultry = 0;
    private int otherAnimals = 0;

    private Map<String, Integer> detailedTypeCounts = new LinkedHashMap<>();

    public AnimalStats() {
    }

    public int getTotalAnimals() {
        return totalAnimals;
    }

    public void setTotalAnimals(int totalAnimals) {
        this.totalAnimals = totalAnimals;
    }

    public int getTotalOwners() {
        return totalOwners;
    }

    public void setTotalOwners(int totalOwners) {
        this.totalOwners = totalOwners;
    }

    public int getTotalSurveys() {
        return totalSurveys;
    }

    public void setTotalSurveys(int totalSurveys) {
        this.totalSurveys = totalSurveys;
    }

    public int getTotalLivestock() {
        return totalLivestock;
    }

    public void setTotalLivestock(int totalLivestock) {
        this.totalLivestock = totalLivestock;
    }

    public int getTotalPets() {
        return totalPets;
    }

    public void setTotalPets(int totalPets) {
        this.totalPets = totalPets;
    }

    public int getTotalPoultry() {
        return totalPoultry;
    }

    public void setTotalPoultry(int totalPoultry) {
        this.totalPoultry = totalPoultry;
    }

    public int getTotalOther() {
        return totalOther;
    }

    public void setTotalOther(int totalOther) {
        this.totalOther = totalOther;
    }

    public int getCows() {
        return cows;
    }

    public void setCows(int cows) {
        this.cows = cows;
    }

    public int getBuffaloes() {
        return buffaloes;
    }

    public void setBuffaloes(int buffaloes) {
        this.buffaloes = buffaloes;
    }

    public int getGoats() {
        return goats;
    }

    public void setGoats(int goats) {
        this.goats = goats;
    }

    public int getSheep() {
        return sheep;
    }

    public void setSheep(int sheep) {
        this.sheep = sheep;
    }

    public int getDogs() {
        return dogs;
    }

    public void setDogs(int dogs) {
        this.dogs = dogs;
    }

    public int getCats() {
        return cats;
    }

    public void setCats(int cats) {
        this.cats = cats;
    }

    public int getPoultry() {
        return poultry;
    }

    public void setPoultry(int poultry) {
        this.poultry = poultry;
    }

    public int getOtherAnimals() {
        return otherAnimals;
    }

    public void setOtherAnimals(int otherAnimals) {
        this.otherAnimals = otherAnimals;
    }

    public Map<String, Integer> getDetailedTypeCounts() {
        return detailedTypeCounts;
    }

    public void setDetailedTypeCounts(Map<String, Integer> detailedTypeCounts) {
        this.detailedTypeCounts = detailedTypeCounts;
    }
}
