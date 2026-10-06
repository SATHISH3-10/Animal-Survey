package com.example.vilage_wise.repository;

import androidx.annotation.NonNull;

import com.example.vilage_wise.models.AnimalStats;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.OwnerAnimalCount;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.utils.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FirebaseRepository {
    private static FirebaseRepository instance;
    private final FirebaseFirestore db;
    private final CollectionReference villagesRef;
    private final CollectionReference ownersRef;
    private final CollectionReference surveysRef;
    private final CollectionReference usersRef;

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface DuplicateCheckCallback {
        void onResult(boolean isDuplicate, String message);
        void onError(Exception e);
    }

    public interface DeleteSafetyCallback {
        void onResult(boolean canDeleteSafely, int associatedOwners, int associatedSurveys);
        void onError(Exception e);
    }

    public interface ResolveCallback {
        void onResolved(Village village, Owner owner);
        void onError(Exception e);
    }

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
        try {
            com.google.firebase.firestore.FirebaseFirestoreSettings settings =
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .build();
            db.setFirestoreSettings(settings);
        } catch (Exception ignored) {
        }
        villagesRef = db.collection(Constants.COLLECTION_VILLAGES);
        ownersRef = db.collection(Constants.COLLECTION_OWNERS);
        surveysRef = db.collection(Constants.COLLECTION_SURVEYS);
        usersRef = db.collection(Constants.COLLECTION_USERS);
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Resolves Village and Owner from typed names.
     * If they don't exist in Firestore, creates them seamlessly.
     */
    public void resolveVillageAndOwner(String villageName, String district, String state, 
                                       String ownerName, String ownerContact, ResolveCallback callback) {
        String finalVillageName = villageName.trim();
        String finalDistrict = (district != null && !district.trim().isEmpty()) ? district.trim() : "Main District";
        String finalState = (state != null && !state.trim().isEmpty()) ? state.trim() : "State";
        String finalOwnerName = ownerName.trim();
        String finalContact = (ownerContact != null) ? ownerContact.trim() : "";

        // 1. Find or create Village
        villagesRef.get().addOnSuccessListener(villageSnapshots -> {
            Village targetVillage = null;
            for (DocumentSnapshot doc : villageSnapshots) {
                Village v = doc.toObject(Village.class);
                if (v != null && v.getName().trim().equalsIgnoreCase(finalVillageName)) {
                    v.setId(doc.getId());
                    targetVillage = v;
                    break;
                }
            }

            if (targetVillage == null) {
                // Create new Village
                DocumentReference newVRef = villagesRef.document();
                targetVillage = new Village(newVRef.getId(), finalVillageName, finalDistrict, finalState, "", System.currentTimeMillis());
                newVRef.set(targetVillage);
            }

            final Village resolvedVillage = targetVillage;

            // 2. Find or create Owner under this village
            ownersRef.whereEqualTo("villageId", resolvedVillage.getId()).get().addOnSuccessListener(ownerSnapshots -> {
                Owner targetOwner = null;
                for (DocumentSnapshot doc : ownerSnapshots) {
                    Owner o = doc.toObject(Owner.class);
                    if (o != null && o.getName().trim().equalsIgnoreCase(finalOwnerName)) {
                        o.setId(doc.getId());
                        targetOwner = o;
                        break;
                    }
                }

                if (targetOwner == null) {
                    // Create new Owner
                    DocumentReference newORef = ownersRef.document();
                    targetOwner = new Owner(newORef.getId(), finalOwnerName, finalContact, resolvedVillage.getId(), resolvedVillage.getName(), "", System.currentTimeMillis());
                    newORef.set(targetOwner);
                }

                if (callback != null) {
                    callback.onResolved(resolvedVillage, targetOwner);
                }
            }).addOnFailureListener(e -> {
                // Fallback: create owner directly
                DocumentReference newORef = ownersRef.document();
                Owner newOwner = new Owner(newORef.getId(), finalOwnerName, finalContact, resolvedVillage.getId(), resolvedVillage.getName(), "", System.currentTimeMillis());
                newORef.set(newOwner);
                if (callback != null) {
                    callback.onResolved(resolvedVillage, newOwner);
                }
            });

        }).addOnFailureListener(e -> {
            // Offline / fallback creation
            DocumentReference newVRef = villagesRef.document();
            Village fallbackVillage = new Village(newVRef.getId(), finalVillageName, finalDistrict, finalState, "", System.currentTimeMillis());
            newVRef.set(fallbackVillage);

            DocumentReference newORef = ownersRef.document();
            Owner fallbackOwner = new Owner(newORef.getId(), finalOwnerName, finalContact, fallbackVillage.getId(), fallbackVillage.getName(), "", System.currentTimeMillis());
            newORef.set(fallbackOwner);

            if (callback != null) {
                callback.onResolved(fallbackVillage, fallbackOwner);
            }
        });
    }

    // ==========================================
    // VILLAGE OPERATIONS
    // ==========================================

    public void addVillage(Village village, SimpleCallback callback) {
        DocumentReference docRef = villagesRef.document();
        village.setId(docRef.getId());
        if (village.getCreatedAt() == 0) {
            village.setCreatedAt(System.currentTimeMillis());
        }
        docRef.set(village)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void updateVillage(Village village, SimpleCallback callback) {
        if (village.getId() == null || village.getId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Village ID cannot be empty"));
            return;
        }
        villagesRef.document(village.getId()).set(village)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void checkVillageDuplicate(String name, String code, String excludeVillageId, DuplicateCheckCallback callback) {
        villagesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean duplicate = false;
            String reason = "";
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.getId().equals(excludeVillageId)) {
                    continue;
                }
                Village v = doc.toObject(Village.class);
                if (v != null) {
                    if (name != null && !name.trim().isEmpty() && name.trim().equalsIgnoreCase(v.getName().trim())) {
                        duplicate = true;
                        reason = "A village named '" + name + "' already exists.";
                        break;
                    }
                    if (code != null && !code.trim().isEmpty() && v.getCode() != null && code.trim().equalsIgnoreCase(v.getCode().trim())) {
                        duplicate = true;
                        reason = "A village with code '" + code + "' already exists.";
                        break;
                    }
                }
            }
            callback.onResult(duplicate, reason);
        }).addOnFailureListener(callback::onError);
    }

    public void checkVillageCanDelete(String villageId, DeleteSafetyCallback callback) {
        ownersRef.whereEqualTo("villageId", villageId).get().addOnSuccessListener(ownerDocs -> {
            int ownerCount = ownerDocs.size();
            surveysRef.whereEqualTo("villageId", villageId).get().addOnSuccessListener(surveyDocs -> {
                int surveyCount = surveyDocs.size();
                boolean canDeleteSafely = (ownerCount == 0 && surveyCount == 0);
                callback.onResult(canDeleteSafely, ownerCount, surveyCount);
            }).addOnFailureListener(callback::onError);
        }).addOnFailureListener(callback::onError);
    }

    public void deleteVillage(String villageId, SimpleCallback callback) {
        villagesRef.document(villageId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public ListenerRegistration listenToVillages(DataCallback<List<Village>> callback) {
        return villagesRef.orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (callback != null) callback.onError(e);
                        return;
                    }
                    List<Village> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Village v = doc.toObject(Village.class);
                            if (v != null) {
                                v.setId(doc.getId());
                                list.add(v);
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                });
    }

    // ==========================================
    // OWNER OPERATIONS
    // ==========================================

    public void addOwner(Owner owner, SimpleCallback callback) {
        DocumentReference docRef = ownersRef.document();
        owner.setId(docRef.getId());
        if (owner.getCreatedAt() == 0) {
            owner.setCreatedAt(System.currentTimeMillis());
        }
        docRef.set(owner)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void updateOwner(Owner owner, SimpleCallback callback) {
        if (owner.getId() == null || owner.getId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Owner ID cannot be empty"));
            return;
        }
        ownersRef.document(owner.getId()).set(owner)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void checkOwnerCanDelete(String ownerId, DeleteSafetyCallback callback) {
        surveysRef.whereEqualTo("ownerId", ownerId).get().addOnSuccessListener(surveyDocs -> {
            int surveyCount = surveyDocs.size();
            callback.onResult(surveyCount == 0, 0, surveyCount);
        }).addOnFailureListener(callback::onError);
    }

    public void deleteOwner(String ownerId, SimpleCallback callback) {
        ownersRef.document(ownerId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public ListenerRegistration listenToOwners(String villageIdFilter, DataCallback<List<Owner>> callback) {
        Query query = ownersRef.orderBy("name", Query.Direction.ASCENDING);
        if (villageIdFilter != null && !villageIdFilter.isEmpty() && !villageIdFilter.equalsIgnoreCase("ALL")) {
            query = query.whereEqualTo("villageId", villageIdFilter);
        }
        return query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                if (callback != null) callback.onError(e);
                return;
            }
            List<Owner> list = new ArrayList<>();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Owner o = doc.toObject(Owner.class);
                    if (o != null) {
                        o.setId(doc.getId());
                        list.add(o);
                    }
                }
            }
            if (callback != null) callback.onSuccess(list);
        });
    }

    public void getOwnersByVillage(String villageId, DataCallback<List<Owner>> callback) {
        Query query = ownersRef.orderBy("name", Query.Direction.ASCENDING);
        if (villageId != null && !villageId.isEmpty() && !villageId.equalsIgnoreCase("ALL")) {
            query = query.whereEqualTo("villageId", villageId);
        }
        query.get().addOnSuccessListener(snapshots -> {
            List<Owner> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Owner o = doc.toObject(Owner.class);
                if (o != null) {
                    o.setId(doc.getId());
                    list.add(o);
                }
            }
            callback.onSuccess(list);
        }).addOnFailureListener(callback::onError);
    }

    public void getOwnerById(String ownerId, DataCallback<Owner> callback) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            if (callback != null) callback.onSuccess(null);
            return;
        }
        ownersRef.document(ownerId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Owner o = doc.toObject(Owner.class);
                if (o != null) {
                    o.setId(doc.getId());
                }
                if (callback != null) callback.onSuccess(o);
            } else {
                if (callback != null) callback.onSuccess(null);
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e);
        });
    }

    // ==========================================
    // SURVEY OPERATIONS
    // ==========================================

    public void addSurvey(AnimalSurvey survey, SimpleCallback callback) {
        DocumentReference docRef = surveysRef.document();
        survey.setId(docRef.getId());
        long now = System.currentTimeMillis();
        if (survey.getCreatedAt() == 0) {
            survey.setCreatedAt(now);
        }
        survey.setUpdatedAt(now);
        docRef.set(survey)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void addMultipleSurveys(List<AnimalSurvey> surveys, SimpleCallback callback) {
        if (surveys == null || surveys.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }
        WriteBatch batch = db.batch();
        long now = System.currentTimeMillis();
        for (AnimalSurvey survey : surveys) {
            DocumentReference docRef = surveysRef.document();
            survey.setId(docRef.getId());
            if (survey.getCreatedAt() == 0) {
                survey.setCreatedAt(now);
            }
            survey.setUpdatedAt(now);
            batch.set(docRef, survey);
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    /**
     * Atomically saves village (if new), owner (if new), and all survey records in a single batch.
     * Prevents network delays and avoids nested .get() queries.
     */
    public void saveSurveyComplete(Village village, boolean isNewVillage, 
                                   Owner owner, boolean isNewOwner, 
                                   List<AnimalSurvey> surveys, SimpleCallback callback) {
        WriteBatch batch = db.batch();
        long now = System.currentTimeMillis();

        if (isNewVillage && village != null) {
            if (village.getId() == null || village.getId().isEmpty()) {
                village.setId(villagesRef.document().getId());
            }
            if (village.getCreatedAt() == 0) village.setCreatedAt(now);
            batch.set(villagesRef.document(village.getId()), village);
        }

        if (isNewOwner && owner != null) {
            if (owner.getId() == null || owner.getId().isEmpty()) {
                owner.setId(ownersRef.document().getId());
            }
            if (owner.getCreatedAt() == 0) owner.setCreatedAt(now);
            batch.set(ownersRef.document(owner.getId()), owner);
        }

        if (surveys != null) {
            for (AnimalSurvey s : surveys) {
                if (s.getId() == null || s.getId().trim().isEmpty()) {
                    s.setId(surveysRef.document().getId());
                }
                if (s.getCreatedAt() == 0) s.setCreatedAt(now);
                s.setUpdatedAt(now);
                batch.set(surveysRef.document(s.getId()), s);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void updateSurvey(AnimalSurvey survey, SimpleCallback callback) {
        if (survey.getId() == null || survey.getId().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Survey ID cannot be empty"));
            return;
        }
        survey.setUpdatedAt(System.currentTimeMillis());
        surveysRef.document(survey.getId()).set(survey)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void deleteSurvey(String surveyId, SimpleCallback callback) {
        surveysRef.document(surveyId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public ListenerRegistration listenToSurveys(DataCallback<List<AnimalSurvey>> callback) {
        return listenToSurveys(null, callback);
    }

    public ListenerRegistration listenToSurveys(String villageIdFilter, DataCallback<List<AnimalSurvey>> callback) {
        Query query = surveysRef.orderBy("surveyDate", Query.Direction.DESCENDING);
        if (villageIdFilter != null && !villageIdFilter.isEmpty() && !villageIdFilter.equalsIgnoreCase("ALL")) {
            query = query.whereEqualTo("villageId", villageIdFilter);
        }
        return query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                if (callback != null) callback.onError(e);
                return;
            }
            List<AnimalSurvey> list = new ArrayList<>();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    AnimalSurvey s = doc.toObject(AnimalSurvey.class);
                    if (s != null) {
                        s.setId(doc.getId());
                        list.add(s);
                    }
                }
            }
            if (callback != null) callback.onSuccess(list);
        });
    }

    public void getSurveysByOwner(String ownerId, DataCallback<List<AnimalSurvey>> callback) {
        surveysRef.whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<AnimalSurvey> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        AnimalSurvey s = doc.toObject(AnimalSurvey.class);
                        if (s != null) {
                            s.setId(doc.getId());
                            list.add(s);
                        }
                    }
                    // Sort descending by date
                    list.sort((a, b) -> Long.compare(b.getSurveyDate(), a.getSurveyDate()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // STATISTICAL CALCULATIONS
    // ==========================================

    public static AnimalStats calculateStats(List<AnimalSurvey> surveys, List<Owner> owners) {
        AnimalStats stats = new AnimalStats();
        if (surveys == null) {
            return stats;
        }

        int totalAnimals = 0;
        int cows = 0;
        int buffaloes = 0;
        int goats = 0;
        int sheep = 0;
        int dogs = 0;
        int cats = 0;
        int poultry = 0;
        int otherAnimals = 0;

        int totalLivestock = 0;
        int totalPets = 0;
        int totalPoultryCategory = 0;
        int totalOtherCategory = 0;

        Set<String> surveyedOwnerIds = new HashSet<>();
        Map<String, Integer> detailedTypeCounts = new LinkedHashMap<>();

        for (AnimalSurvey s : surveys) {
            int qty = s.getQuantity();
            if (qty <= 0) continue;

            totalAnimals += qty;
            if (s.getOwnerId() != null && !s.getOwnerId().isEmpty()) {
                surveyedOwnerIds.add(s.getOwnerId());
            }

            String type = s.getDisplayAnimalType().trim();
            String cat = s.getAnimalCategory() != null ? s.getAnimalCategory().trim() : "";

            // Update detailed type map
            if (!type.isEmpty()) {
                int currentTypeCount = detailedTypeCounts.getOrDefault(type, 0);
                detailedTypeCounts.put(type, currentTypeCount + qty);
            }

            // Category counts
            if (Constants.CATEGORY_LIVESTOCK.equalsIgnoreCase(cat)) {
                totalLivestock += qty;
            } else if (Constants.CATEGORY_PETS.equalsIgnoreCase(cat)) {
                totalPets += qty;
            } else if (Constants.CATEGORY_POULTRY.equalsIgnoreCase(cat)) {
                totalPoultryCategory += qty;
            } else {
                totalOtherCategory += qty;
            }

            // Standard animal type counts
            String lowerType = type.toLowerCase(Locale.ROOT);
            if (lowerType.contains("cow")) {
                cows += qty;
            } else if (lowerType.contains("buffalo")) {
                buffaloes += qty;
            } else if (lowerType.contains("goat")) {
                goats += qty;
            } else if (lowerType.contains("sheep")) {
                sheep += qty;
            } else if (lowerType.contains("dog")) {
                dogs += qty;
            } else if (lowerType.contains("cat")) {
                cats += qty;
            } else if (lowerType.contains("chicken") || lowerType.contains("duck") || lowerType.contains("poultry")) {
                poultry += qty;
            } else {
                otherAnimals += qty;
            }
        }

        stats.setTotalAnimals(totalAnimals);
        stats.setTotalSurveys(surveys.size());
        stats.setTotalOwners(owners != null ? owners.size() : surveyedOwnerIds.size());
        stats.setTotalLivestock(totalLivestock);
        stats.setTotalPets(totalPets);
        stats.setTotalPoultry(totalPoultryCategory);
        stats.setTotalOther(totalOtherCategory);

        stats.setCows(cows);
        stats.setBuffaloes(buffaloes);
        stats.setGoats(goats);
        stats.setSheep(sheep);
        stats.setDogs(dogs);
        stats.setCats(cats);
        stats.setPoultry(poultry);
        stats.setOtherAnimals(otherAnimals);
        stats.setDetailedTypeCounts(detailedTypeCounts);

        return stats;
    }

    public static List<OwnerAnimalCount> groupSurveysByOwnerForAnimal(List<AnimalSurvey> surveys, String animalTypeFilter) {
        List<OwnerAnimalCount> result = new ArrayList<>();
        if (surveys == null || animalTypeFilter == null) {
            return result;
        }

        Map<String, OwnerAnimalCount> map = new HashMap<>();

        for (AnimalSurvey s : surveys) {
            String type = s.getDisplayAnimalType();
            String cat = s.getAnimalCategory();

            boolean match = false;
            if (animalTypeFilter.equalsIgnoreCase("ALL")) {
                match = true;
            } else if (type != null && type.equalsIgnoreCase(animalTypeFilter)) {
                match = true;
            } else if (type != null && type.toLowerCase(Locale.ROOT).contains(animalTypeFilter.toLowerCase(Locale.ROOT))) {
                match = true;
            } else if (cat != null && cat.equalsIgnoreCase(animalTypeFilter)) {
                match = true;
            }

            if (!match) continue;

            String ownerId = s.getOwnerId() != null && !s.getOwnerId().isEmpty() ? s.getOwnerId() : "unknown_" + s.getOwnerName();
            OwnerAnimalCount count = map.get(ownerId);
            if (count == null) {
                count = new OwnerAnimalCount(
                        s.getOwnerId(),
                        s.getOwnerName(),
                        s.getVillageId(),
                        s.getVillageName(),
                        animalTypeFilter,
                        s.getQuantity(),
                        1
                );
                map.put(ownerId, count);
            } else {
                count.setTotalCount(count.getTotalCount() + s.getQuantity());
                count.setSurveyRecordsCount(count.getSurveyRecordsCount() + 1);
            }
        }

        result.addAll(map.values());
        // Sort descending by count, then by owner name
        result.sort((a, b) -> {
            int comp = Integer.compare(b.getTotalCount(), a.getTotalCount());
            if (comp != 0) return comp;
            return a.getOwnerName().compareToIgnoreCase(b.getOwnerName());
        });

        return result;
    }

    // ==========================================
    // USER AUTHENTICATION & OFFICER MANAGEMENT
    // ==========================================

    public void registerOfficer(User user, String password, SimpleCallback callback) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty() || password == null || password.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Email and password cannot be empty."));
            return;
        }

        user.setPassword(password);
        user.setRole(Constants.ROLE_OFFICER);
        user.setStatus(Constants.STATUS_PENDING);
        if (user.getCreatedAt() == 0) {
            user.setCreatedAt(System.currentTimeMillis());
        }

        // Try Firebase Auth first
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(user.getEmail().trim(), password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null ? authResult.getUser().getUid() : usersRef.document().getId();
                    user.setUid(uid);
                    usersRef.document(uid).set(user)
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    // Fallback to Firestore directly if Email/Password provider is disabled or unavailable
                    usersRef.whereEqualTo("email", user.getEmail().trim()).get()
                            .addOnSuccessListener(queryDocs -> {
                                if (queryDocs != null && !queryDocs.isEmpty()) {
                                    if (callback != null) callback.onError(new Exception("This email is already registered."));
                                    return;
                                }
                                DocumentReference newDoc = usersRef.document();
                                user.setUid(newDoc.getId());
                                newDoc.set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            if (callback != null) callback.onSuccess();
                                        })
                                        .addOnFailureListener(err -> {
                                            if (callback != null) callback.onError(err);
                                        });
                            })
                            .addOnFailureListener(err -> {
                                DocumentReference newDoc = usersRef.document();
                                user.setUid(newDoc.getId());
                                newDoc.set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            if (callback != null) callback.onSuccess();
                                        })
                                        .addOnFailureListener(callback::onError);
                            });
                });
    }

    public void loginUser(String email, String password, DataCallback<User> callback) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Email and password are required."));
            return;
        }

        final String cleanEmail = email.trim();
        final String cleanPassword = password.trim();

        // Built-in Admin bypass for instant zero-config login
        if (cleanEmail.equalsIgnoreCase("admin@villagewise.gov") && cleanPassword.equals("admin123")) {
            User admin = new User(
                    "admin_master_uid",
                    "Chief Admin Officer",
                    cleanEmail,
                    "9999999999",
                    "ADMIN-01",
                    "All Villages",
                    "System Administrator",
                    Constants.ROLE_ADMIN,
                    Constants.STATUS_APPROVED,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    "System"
            );
            admin.setPassword("admin123");
            usersRef.document("admin_master_uid").set(admin);
            if (callback != null) callback.onSuccess(admin);
            return;
        }

        // 1. Try Firebase Auth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(cleanEmail, cleanPassword)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) {
                        fallbackFirestoreLogin(cleanEmail, cleanPassword, callback);
                        return;
                    }
                    String uid = authResult.getUser().getUid();
                    usersRef.document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    User user = doc.toObject(User.class);
                                    if (user != null) {
                                        user.setUid(doc.getId());
                                        if (callback != null) callback.onSuccess(user);
                                        return;
                                    }
                                }
                                fallbackFirestoreLogin(cleanEmail, cleanPassword, callback);
                            })
                            .addOnFailureListener(e -> fallbackFirestoreLogin(cleanEmail, cleanPassword, callback));
                })
                .addOnFailureListener(e -> {
                    // Fallback to Firestore users collection
                    fallbackFirestoreLogin(cleanEmail, cleanPassword, callback);
                });
    }

    private void fallbackFirestoreLogin(String email, String password, DataCallback<User> callback) {
        usersRef.whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocs -> {
                    if (queryDocs == null || queryDocs.isEmpty()) {
                        if (callback != null) callback.onError(new Exception("Invalid email or password."));
                        return;
                    }

                    boolean foundMatch = false;
                    for (DocumentSnapshot doc : queryDocs.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            String storedPass = user.getPassword();
                            if (storedPass == null || storedPass.isEmpty() || storedPass.equals(password)) {
                                foundMatch = true;
                                if (callback != null) callback.onSuccess(user);
                                break;
                            }
                        }
                    }

                    if (!foundMatch) {
                        if (callback != null) callback.onError(new Exception("Incorrect password. Please try again."));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(new Exception("Login failed: " + e.getMessage()));
                });
    }

    public ListenerRegistration listenToUsers(DataCallback<List<User>> callback) {
        return usersRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (callback != null) callback.onError(e);
                        return;
                    }
                    List<User> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                u.setUid(doc.getId());
                                list.add(u);
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                });
    }

    public void approveOfficerWithBadge(String uid, String badgeId, String approvedBy, SimpleCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("User ID is required."));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Constants.STATUS_APPROVED);
        updates.put("officerBadgeId", badgeId);
        updates.put("approvedAt", System.currentTimeMillis());
        updates.put("approvedBy", approvedBy != null ? approvedBy : "Admin");

        usersRef.document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void updateUserStatus(String uid, String newStatus, String approvedBy, SimpleCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("User ID is required."));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("approvedAt", System.currentTimeMillis());
        updates.put("approvedBy", approvedBy != null ? approvedBy : "Admin");

        usersRef.document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void deleteUser(String uid, SimpleCallback callback) {
        if (uid == null || uid.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("User ID is required."));
            return;
        }
        usersRef.document(uid).delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void ensureAdminAccount(String adminEmail, String adminPassword, SimpleCallback callback) {
        User admin = new User(
                "admin_master_uid",
                "Chief Admin Officer",
                adminEmail,
                "9999999999",
                "ADMIN-01",
                "All Villages",
                "Administrator",
                Constants.ROLE_ADMIN,
                Constants.STATUS_APPROVED,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "System"
        );
        admin.setPassword(adminPassword);
        usersRef.document("admin_master_uid").set(admin)
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onSuccess(); // Non-blocking
                });
    }
}

