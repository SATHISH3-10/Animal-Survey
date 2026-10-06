package com.example.vilage_wise.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String COLLECTION_VILLAGES = "villages";
    public static final String COLLECTION_OWNERS = "owners";
    public static final String COLLECTION_SURVEYS = "animal_surveys";
    public static final String COLLECTION_USERS = "users";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OFFICER = "OFFICER";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String CATEGORY_LIVESTOCK = "Livestock";
    public static final String CATEGORY_PETS = "Pets";
    public static final String CATEGORY_POULTRY = "Poultry";
    public static final String CATEGORY_OTHER = "Other animals";

    public static final String[] CATEGORIES = new String[]{
            CATEGORY_LIVESTOCK,
            CATEGORY_PETS,
            CATEGORY_POULTRY,
            CATEGORY_OTHER
    };

    public static final String[] LIVESTOCK_TYPES = new String[]{
            "Cows", "Buffaloes", "Goats", "Sheep", "Pigs", "Other Livestock"
    };

    public static final String[] PET_TYPES = new String[]{
            "Dogs", "Cats", "Pet birds", "Other pets"
    };

    public static final String[] POULTRY_TYPES = new String[]{
            "Chickens", "Ducks", "Other poultry"
    };

    public static final String[] ALL_COMMON_TYPES = new String[]{
            "Cows", "Buffaloes", "Goats", "Sheep", "Dogs", "Cats", "Chickens", "Ducks", "Pigs", "Pet birds", "Other"
    };

    public static List<String> getTypesForCategory(String category) {
        if (CATEGORY_LIVESTOCK.equalsIgnoreCase(category)) {
            return Arrays.asList(LIVESTOCK_TYPES);
        } else if (CATEGORY_PETS.equalsIgnoreCase(category)) {
            return Arrays.asList(PET_TYPES);
        } else if (CATEGORY_POULTRY.equalsIgnoreCase(category)) {
            return Arrays.asList(POULTRY_TYPES);
        } else {
            List<String> list = new ArrayList<>();
            list.add("Other");
            return list;
        }
    }
}
