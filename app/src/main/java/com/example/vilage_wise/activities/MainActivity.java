package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.R;
import com.example.vilage_wise.adapters.SurveyAdapter;
import com.example.vilage_wise.databinding.ActivityMainBinding;
import com.example.vilage_wise.models.AnimalStats;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseRepository repository;
    private SessionManager sessionManager;
    private SurveyAdapter surveyAdapter;

    private List<Village> villageList = new ArrayList<>();
    private List<Owner> ownerList = new ArrayList<>();
    private List<AnimalSurvey> surveyList = new ArrayList<>();

    private String selectedVillageId = "ALL";
    private String selectedVillageName = "All Villages";

    private ListenerRegistration villagesListener;
    private ListenerRegistration ownersListener;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setupUI();
        setupListeners();
    }

    private void setupUI() {
        // Setup Officer/Admin User Information Tag
        if (sessionManager.isAdmin()) {
            binding.tvOfficerHeaderTag.setText("Admin: " + sessionManager.getUserName());
            binding.btnAdminSpace.setVisibility(View.VISIBLE);
            binding.btnAdminSpace.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
            });
        } else {
            String badge = sessionManager.getBadgeId();
            String name = sessionManager.getUserName();
            String village = sessionManager.getAssignedVillage();
            String tag = "Officer: " + name + (!badge.isEmpty() ? " (" + badge + ")" : "");
            if (!village.isEmpty()) {
                tag += " • " + village;
            }
            binding.tvOfficerHeaderTag.setText(tag);
            binding.tvOfficerHeaderTag.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, OfficerProfileActivity.class));
            });
            binding.btnAdminSpace.setVisibility(View.GONE);
        }

        binding.btnOfficerProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, OfficerProfileActivity.class));
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Setup recent surveys RecyclerView
        surveyAdapter = new SurveyAdapter(survey -> {
            Intent intent = new Intent(MainActivity.this, SurveyDetailActivity.class);
            intent.putExtra("survey", survey);
            startActivity(intent);
        });
        binding.rvRecentSurveys.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentSurveys.setAdapter(surveyAdapter);

        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            restartListeners();
            binding.swipeRefresh.setRefreshing(false);
        });

        // Top Navigation buttons
        binding.btnNavVillages.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, VillageListActivity.class));
        });

        binding.btnNavOwners.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OwnerListActivity.class);
            if (!"ALL".equals(selectedVillageId)) {
                intent.putExtra("villageId", selectedVillageId);
            }
            startActivity(intent);
        });

        binding.btnNavSurveys.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SurveyListActivity.class);
            if (!"ALL".equals(selectedVillageId)) {
                intent.putExtra("villageId", selectedVillageId);
            }
            startActivity(intent);
        });

        binding.tvViewAllSurveys.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SurveyListActivity.class);
            if (!"ALL".equals(selectedVillageId)) {
                intent.putExtra("villageId", selectedVillageId);
            }
            startActivity(intent);
        });

        binding.tvViewAllStats.setOnClickListener(v -> openDetailedStats());
        binding.btnStatsDetailed.setOnClickListener(v -> openDetailedStats());

        // Floating Action Button: Add Survey
        binding.fabAddSurvey.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SurveyFormActivity.class);
            if (!"ALL".equals(selectedVillageId)) {
                intent.putExtra("preselectedVillageId", selectedVillageId);
            }
            startActivity(intent);
        });

        // Setup Card Clicks -> Open Owner Breakdown
        binding.cardTotalAnimals.setOnClickListener(v -> openGroupedOwners("ALL", "All Animals"));
        binding.cardTotalOwners.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OwnerListActivity.class);
            if (!"ALL".equals(selectedVillageId)) {
                intent.putExtra("villageId", selectedVillageId);
            }
            startActivity(intent);
        });

        binding.cardCows.setOnClickListener(v -> openGroupedOwners("Cows", "Cows"));
        binding.cardBuffaloes.setOnClickListener(v -> openGroupedOwners("Buffaloes", "Buffaloes"));
        binding.cardGoats.setOnClickListener(v -> openGroupedOwners("Goats", "Goats"));
        binding.cardSheep.setOnClickListener(v -> openGroupedOwners("Sheep", "Sheep"));
        binding.cardDogs.setOnClickListener(v -> openGroupedOwners("Dogs", "Dogs"));
        binding.cardCats.setOnClickListener(v -> openGroupedOwners("Cats", "Cats"));
        binding.cardPoultry.setOnClickListener(v -> openGroupedOwners("Poultry", "Poultry"));
        binding.cardOtherAnimals.setOnClickListener(v -> openGroupedOwners("Other", "Other Animals"));
    }

    private void openGroupedOwners(String animalType, String title) {
        Intent intent = new Intent(MainActivity.this, AnimalGroupedOwnersActivity.class);
        intent.putExtra("animalType", animalType);
        intent.putExtra("title", title);
        intent.putExtra("villageId", selectedVillageId);
        intent.putExtra("villageName", selectedVillageName);
        startActivity(intent);
    }

    private void openDetailedStats() {
        Intent intent = new Intent(MainActivity.this, VillageStatisticsActivity.class);
        intent.putExtra("villageId", selectedVillageId);
        intent.putExtra("villageName", selectedVillageName);
        startActivity(intent);
    }

    private void setupListeners() {
        // Listen to Villages
        villagesListener = repository.listenToVillages(new FirebaseRepository.DataCallback<List<Village>>() {
            @Override
            public void onSuccess(List<Village> data) {
                villageList = data != null ? data : new ArrayList<>();
                updateVillageSpinner();
                recalculate();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error syncing villages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Listen to Owners
        ownersListener = repository.listenToOwners(null, new FirebaseRepository.DataCallback<List<Owner>>() {
            @Override
            public void onSuccess(List<Owner> data) {
                ownerList = data != null ? data : new ArrayList<>();
                recalculate();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error syncing owners: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Listen to Surveys
        surveysListener = repository.listenToSurveys(null, new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                surveyList = data != null ? data : new ArrayList<>();
                recalculate();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error syncing surveys: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restartListeners() {
        if (villagesListener != null) villagesListener.remove();
        if (ownersListener != null) ownersListener.remove();
        if (surveysListener != null) surveysListener.remove();
        setupListeners();
    }

    private void updateVillageSpinner() {
        List<String> spinnerLabels = new ArrayList<>();
        spinnerLabels.add("All Villages (Overview)");

        for (Village v : villageList) {
            String label = v.getName();
            if (v.getDistrict() != null && !v.getDistrict().isEmpty()) {
                label += " (" + v.getDistrict() + ")";
            }
            spinnerLabels.add(label);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerVillage.setAdapter(adapter);

        binding.spinnerVillage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedVillageId = "ALL";
                    selectedVillageName = "All Villages";
                } else {
                    Village v = villageList.get(position - 1);
                    selectedVillageId = v.getId();
                    selectedVillageName = v.getName();
                }
                recalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void recalculate() {
        // Filter by selected village if not ALL
        List<AnimalSurvey> filteredSurveys = new ArrayList<>();
        List<Owner> filteredOwners = new ArrayList<>();

        for (AnimalSurvey s : surveyList) {
            if ("ALL".equals(selectedVillageId) || (s.getVillageId() != null && s.getVillageId().equals(selectedVillageId))) {
                filteredSurveys.add(s);
            }
        }

        for (Owner o : ownerList) {
            if ("ALL".equals(selectedVillageId) || (o.getVillageId() != null && o.getVillageId().equals(selectedVillageId))) {
                filteredOwners.add(o);
            }
        }

        AnimalStats stats = FirebaseRepository.calculateStats(filteredSurveys, filteredOwners);

        // Update UI summary counts
        binding.tvTotalAnimalsCount.setText(String.valueOf(stats.getTotalAnimals()));
        binding.tvTotalSurveysCount.setText("From " + filteredSurveys.size() + " survey records");

        binding.tvTotalOwnersCount.setText(String.valueOf(filteredOwners.size()));
        if ("ALL".equals(selectedVillageId)) {
            binding.tvVillagesCount.setText("Across " + villageList.size() + " villages");
        } else {
            binding.tvVillagesCount.setText("In " + selectedVillageName);
        }

        // Animal Cards
        binding.tvCowsCount.setText(String.valueOf(stats.getCows()));
        binding.tvBuffaloesCount.setText(String.valueOf(stats.getBuffaloes()));
        binding.tvGoatsCount.setText(String.valueOf(stats.getGoats()));
        binding.tvSheepCount.setText(String.valueOf(stats.getSheep()));
        binding.tvDogsCount.setText(String.valueOf(stats.getDogs()));
        binding.tvCatsCount.setText(String.valueOf(stats.getCats()));
        binding.tvPoultryCount.setText(String.valueOf(stats.getPoultry()));
        binding.tvOtherCount.setText(String.valueOf(stats.getOtherAnimals()));

        // Category breakdown
        binding.tvCatLivestockCount.setText(stats.getTotalLivestock() + " heads");
        binding.tvCatPetsCount.setText(stats.getTotalPets() + " pets");
        binding.tvCatPoultryCount.setText(stats.getTotalPoultry() + " birds");

        // Recent Surveys list (show top 5 recent)
        List<AnimalSurvey> recentList = new ArrayList<>();
        int maxRecent = Math.min(filteredSurveys.size(), 5);
        for (int i = 0; i < maxRecent; i++) {
            recentList.add(filteredSurveys.get(i));
        }

        if (recentList.isEmpty()) {
            binding.layoutEmptySurveys.setVisibility(View.VISIBLE);
            binding.rvRecentSurveys.setVisibility(View.GONE);
        } else {
            binding.layoutEmptySurveys.setVisibility(View.GONE);
            binding.rvRecentSurveys.setVisibility(View.VISIBLE);
            surveyAdapter.setSurveys(recentList);
        }
    }

    private void showOfficerProfileDialog() {
        User user = sessionManager.getCachedUser();
        if (user == null) return;

        String msg = "👤 Officer Name: " + user.getFullName() +
                     "\n🎖 Official Badge ID: " + (user.getOfficerBadgeId().isEmpty() ? "Assigned" : user.getOfficerBadgeId()) +
                     "\n📍 Assigned Region: " + (user.getAssignedVillage().isEmpty() ? "All Villages" : user.getAssignedVillage()) +
                     "\n📧 Email: " + user.getEmail() +
                     "\n💼 Status: APPROVED Survey Officer";

        new AlertDialog.Builder(this)
                .setTitle("Official Officer Identity")
                .setMessage(msg)
                .setIcon(R.drawable.ic_badge)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Officer Logout")
                .setMessage("Are you sure you want to log out from this session?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
        if (ownersListener != null) ownersListener.remove();
        if (surveysListener != null) surveysListener.remove();
    }
}
