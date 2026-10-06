package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.adapters.OfficerRequestAdapter;
import com.example.vilage_wise.databinding.ActivityAdminDashboardBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.Constants;
import com.example.vilage_wise.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements OfficerRequestAdapter.OnOfficerActionListener {

    private ActivityAdminDashboardBinding binding;
    private FirebaseRepository repository;
    private SessionManager sessionManager;
    private OfficerRequestAdapter adapter;

    private List<User> allOfficers = new ArrayList<>();
    private String currentFilter = Constants.STATUS_PENDING;

    private ListenerRegistration usersListener;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        // Security check: If not admin, redirect
        if (!sessionManager.isLoggedIn() || !sessionManager.isAdmin()) {
            Toast.makeText(this, "Admin access required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupUI();
        setupRecyclerView();
        setupListeners();
        loadData();
    }

    private void setupUI() {
        binding.tvAdminName.setText(sessionManager.getUserName());
        binding.tvAdminEmail.setText(sessionManager.getCachedUser() != null ? sessionManager.getCachedUser().getEmail() : "admin@villagewise.gov");

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());

        binding.btnGoDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            startActivity(intent);
        });

        binding.btnViewAllSurveys.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, SurveyListActivity.class);
            startActivity(intent);
        });

        // Metric Card clicks for quick filter
        binding.cardPendingFilter.setOnClickListener(v -> binding.chipPending.setChecked(true));
        binding.cardApprovedFilter.setOnClickListener(v -> binding.chipApproved.setChecked(true));
        binding.cardSurveysFilter.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, SurveyListActivity.class);
            startActivity(intent);
        });

        // Filter chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == binding.chipPending.getId()) {
                currentFilter = Constants.STATUS_PENDING;
            } else if (checkedId == binding.chipApproved.getId()) {
                currentFilter = Constants.STATUS_APPROVED;
            } else {
                currentFilter = "ALL";
            }
            filterAndDisplay();
        });
    }

    private void setupRecyclerView() {
        adapter = new OfficerRequestAdapter(this);
        binding.rvOfficerRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOfficerRequests.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void loadData() {
        // Listen to Users (Officers)
        usersListener = repository.listenToUsers(new FirebaseRepository.DataCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> data) {
                binding.progressBar.setVisibility(View.GONE);
                allOfficers = data != null ? data : new ArrayList<>();
                updateCounts();
                filterAndDisplay();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this, "Failed to load officers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Listen to total Surveys
        surveysListener = repository.listenToSurveys(new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                int totalSurveys = data != null ? data.size() : 0;
                binding.tvCountSurveys.setText(String.valueOf(totalSurveys));
            }

            @Override
            public void onError(Exception e) {
                // Ignore
            }
        });
    }

    private void updateCounts() {
        int pending = 0;
        int approved = 0;

        for (User u : allOfficers) {
            if (Constants.ROLE_ADMIN.equalsIgnoreCase(u.getRole())) continue;

            if (Constants.STATUS_PENDING.equalsIgnoreCase(u.getStatus())) {
                pending++;
            } else if (Constants.STATUS_APPROVED.equalsIgnoreCase(u.getStatus())) {
                approved++;
            }
        }

        binding.tvCountPending.setText(String.valueOf(pending));
        binding.tvCountApproved.setText(String.valueOf(approved));
    }

    private void filterAndDisplay() {
        List<User> filtered = new ArrayList<>();
        for (User u : allOfficers) {
            // Exclude super admins from officer list
            if (Constants.ROLE_ADMIN.equalsIgnoreCase(u.getRole())) continue;

            if ("ALL".equalsIgnoreCase(currentFilter)) {
                filtered.add(u);
            } else if (currentFilter.equalsIgnoreCase(u.getStatus())) {
                filtered.add(u);
            }
        }

        adapter.setOfficers(filtered);

        if (filtered.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            if (Constants.STATUS_PENDING.equalsIgnoreCase(currentFilter)) {
                binding.tvEmptyMessage.setText("No pending officer registration requests.");
            } else if (Constants.STATUS_APPROVED.equalsIgnoreCase(currentFilter)) {
                binding.tvEmptyMessage.setText("No approved officers yet.");
            } else {
                binding.tvEmptyMessage.setText("No registered officers found.");
            }
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onApprove(User user) {
        // Automatically generate official Officer Badge ID (e.g. VSO-101, VSO-102...)
        String existingBadge = user.getOfficerBadgeId();
        String generatedBadge;
        if (existingBadge != null && !existingBadge.trim().isEmpty() && !existingBadge.equalsIgnoreCase("PENDING")) {
            generatedBadge = existingBadge;
        } else {
            int approvedCount = 0;
            for (User u : allOfficers) {
                if (Constants.STATUS_APPROVED.equalsIgnoreCase(u.getStatus())) {
                    approvedCount++;
                }
            }
            generatedBadge = String.format(java.util.Locale.ROOT, "VSO-%03d", approvedCount + 101);
        }

        final String finalBadge = generatedBadge;

        new AlertDialog.Builder(this)
                .setTitle("Approve Officer Application")
                .setMessage("Authorize " + user.getFullName() + " (" + user.getEmail() + ") as an active Survey Officer for " + user.getAssignedVillage() + "?\n\nOfficial Assigned Badge ID: " + finalBadge)
                .setPositiveButton("Approve & Assign Badge", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    String adminEmail = sessionManager.getCachedUser() != null ? sessionManager.getCachedUser().getEmail() : "Admin";
                    repository.approveOfficerWithBadge(user.getUid(), finalBadge, adminEmail, new FirebaseRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            binding.progressBar.setVisibility(View.GONE);
                            Snackbar.make(binding.getRoot(), user.getFullName() + " approved with Badge ID: " + finalBadge, Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminDashboardActivity.this, "Approval failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onReject(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Registration")
                .setMessage("Are you sure you want to reject registration for " + user.getFullName() + "?")
                .setPositiveButton("Reject", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    String adminEmail = sessionManager.getCachedUser() != null ? sessionManager.getCachedUser().getEmail() : "Admin";
                    repository.updateUserStatus(user.getUid(), Constants.STATUS_REJECTED, adminEmail, new FirebaseRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            binding.progressBar.setVisibility(View.GONE);
                            Snackbar.make(binding.getRoot(), "Registration for " + user.getFullName() + " rejected.", Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminDashboardActivity.this, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Admin Logout")
                .setMessage("Are you sure you want to log out from the Admin Control Center?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
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
        if (usersListener != null) usersListener.remove();
        if (surveysListener != null) surveysListener.remove();
    }
}
