package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.adapters.VillageAdapter;
import com.example.vilage_wise.databinding.ActivityVillageListBinding;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VillageListActivity extends AppCompatActivity {

    private ActivityVillageListBinding binding;
    private FirebaseRepository repository;
    private VillageAdapter adapter;
    private List<Village> allVillages = new ArrayList<>();
    private ListenerRegistration villagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVillageListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.fabAddVillage.setOnClickListener(v -> {
            startActivity(new Intent(VillageListActivity.this, VillageFormActivity.class));
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            setupListeners();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupRecyclerView() {
        adapter = new VillageAdapter(new VillageAdapter.OnVillageClickListener() {
            @Override
            public void onVillageClick(Village village) {
                Intent intent = new Intent(VillageListActivity.this, VillageStatisticsActivity.class);
                intent.putExtra("villageId", village.getId());
                intent.putExtra("villageName", village.getName());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Village village) {
                Intent intent = new Intent(VillageListActivity.this, VillageFormActivity.class);
                intent.putExtra("village", village);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Village village) {
                confirmDeleteVillage(village);
            }
        });

        binding.rvVillages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVillages.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearchVillage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    binding.btnClearSearch.setVisibility(View.GONE);
                    adapter.setVillages(allVillages);
                    updateEmptyState(allVillages.isEmpty());
                } else {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                    filterVillages(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearchVillage.setText(""));
    }

    private void filterVillages(String query) {
        List<Village> filtered = new ArrayList<>();
        for (Village v : allVillages) {
            boolean match = v.getName().toLowerCase(Locale.ROOT).contains(query) ||
                    v.getDistrict().toLowerCase(Locale.ROOT).contains(query) ||
                    v.getCode().toLowerCase(Locale.ROOT).contains(query);
            if (match) {
                filtered.add(v);
            }
        }
        adapter.setVillages(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void setupListeners() {
        binding.progressBar.setVisibility(View.VISIBLE);
        if (villagesListener != null) villagesListener.remove();

        villagesListener = repository.listenToVillages(new FirebaseRepository.DataCallback<List<Village>>() {
            @Override
            public void onSuccess(List<Village> data) {
                binding.progressBar.setVisibility(View.GONE);
                allVillages = data != null ? data : new ArrayList<>();
                String query = binding.etSearchVillage.getText().toString().trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    adapter.setVillages(allVillages);
                    updateEmptyState(allVillages.isEmpty());
                } else {
                    filterVillages(query);
                }
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(VillageListActivity.this, "Error loading villages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvVillages.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvVillages.setVisibility(View.VISIBLE);
        }
    }

    private void confirmDeleteVillage(Village village) {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.checkVillageCanDelete(village.getId(), new FirebaseRepository.DeleteSafetyCallback() {
            @Override
            public void onResult(boolean canDeleteSafely, int associatedOwners, int associatedSurveys) {
                binding.progressBar.setVisibility(View.GONE);
                if (!canDeleteSafely) {
                    new AlertDialog.Builder(VillageListActivity.this)
                            .setTitle("Cannot Delete Village")
                            .setMessage("Village '" + village.getName() + "' has " + associatedOwners +
                                    " registered owners and " + associatedSurveys +
                                    " animal survey records.\n\nPlease delete or reassign all associated owners and surveys before deleting this village.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(VillageListActivity.this)
                            .setTitle("Delete Village")
                            .setMessage("Are you sure you want to permanently delete '" + village.getName() + "'?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                binding.progressBar.setVisibility(View.VISIBLE);
                                repository.deleteVillage(village.getId(), new FirebaseRepository.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        binding.progressBar.setVisibility(View.GONE);
                                        Toast.makeText(VillageListActivity.this, "Village deleted successfully", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        binding.progressBar.setVisibility(View.GONE);
                                        Toast.makeText(VillageListActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(VillageListActivity.this, "Error verifying dependencies: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
    }
}
