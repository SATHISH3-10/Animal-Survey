package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.R;
import com.example.vilage_wise.databinding.ActivityOfficerProfileBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.SessionManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class OfficerProfileActivity extends AppCompatActivity {

    private ActivityOfficerProfileBinding binding;
    private FirebaseRepository repository;
    private SessionManager sessionManager;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOfficerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupUI();
        loadOfficerStats();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        User user = sessionManager.getCachedUser();
        if (user != null) {
            binding.tvOfficerName.setText(user.getFullName().isEmpty() ? "Official Officer" : user.getFullName());
            binding.tvDesignation.setText(user.getDesignation().isEmpty() ? "Village Survey Officer" : user.getDesignation());
            
            String badge = user.getOfficerBadgeId();
            if (badge.isEmpty() || badge.equalsIgnoreCase("PENDING_ASSIGNMENT")) {
                badge = "VSO-101";
            }
            binding.tvBadgePill.setText("OFFICIAL ID: " + badge);

            binding.tvDetailVillage.setText(user.getAssignedVillage().isEmpty() ? "All Assigned Villages" : user.getAssignedVillage());
            binding.tvDetailEmail.setText(user.getEmail().isEmpty() ? "Not specified" : user.getEmail());
            binding.tvDetailPhone.setText(user.getPhone().isEmpty() ? "Not provided" : user.getPhone());
            binding.tvDetailStatus.setText(user.getStatus().toUpperCase() + " (Active Field Officer)");
        }

        binding.btnViewMySurveys.setOnClickListener(v -> {
            Intent intent = new Intent(OfficerProfileActivity.this, SurveyListActivity.class);
            if (user != null && !user.getAssignedVillage().isEmpty()) {
                intent.putExtra("villageName", user.getAssignedVillage());
            }
            startActivity(intent);
        });

        binding.btnLogoutProfile.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadOfficerStats() {
        User user = sessionManager.getCachedUser();
        final String currentUid = user != null ? user.getUid() : "";
        final String currentEmail = user != null ? user.getEmail() : "";
        final String currentVillage = user != null ? user.getAssignedVillage() : "";

        surveysListener = repository.listenToSurveys(new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                if (data == null) return;

                int mySurveys = 0;
                int myAnimals = 0;

                for (AnimalSurvey s : data) {
                    boolean isMine = false;
                    if (s.getSurveyorId() != null && !s.getSurveyorId().isEmpty() && s.getSurveyorId().equals(currentUid)) {
                        isMine = true;
                    } else if (s.getSurveyorEmail() != null && !s.getSurveyorEmail().isEmpty() && s.getSurveyorEmail().equalsIgnoreCase(currentEmail)) {
                        isMine = true;
                    } else if (currentVillage != null && !currentVillage.isEmpty() && currentVillage.equalsIgnoreCase(s.getVillageName())) {
                        isMine = true;
                    }

                    if (isMine) {
                        mySurveys++;
                        myAnimals += s.getQuantity();
                    }
                }

                // If user just started and has 0 individual attributed, fallback to current total if in single office
                if (mySurveys == 0 && !data.isEmpty()) {
                    mySurveys = data.size();
                    for (AnimalSurvey s : data) {
                        myAnimals += s.getQuantity();
                    }
                }

                binding.tvMySurveysCount.setText(String.valueOf(mySurveys));
                binding.tvMyAnimalsCount.setText(String.valueOf(myAnimals));
            }

            @Override
            public void onError(Exception e) {
                // Ignore
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Officer Sign Out")
                .setMessage("Are you sure you want to log out from this session?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(OfficerProfileActivity.this, LoginActivity.class);
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
        if (surveysListener != null) surveysListener.remove();
    }
}
