package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.adapters.OwnerAdapter;
import com.example.vilage_wise.databinding.ActivityOwnerListBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerListActivity extends AppCompatActivity {

    private ActivityOwnerListBinding binding;
    private FirebaseRepository repository;
    private OwnerAdapter adapter;

    private List<Village> villageList = new ArrayList<>();
    private List<Owner> allOwners = new ArrayList<>();
    private List<AnimalSurvey> allSurveys = new ArrayList<>();

    private String selectedVillageId = "ALL";
    private ListenerRegistration villagesListener;
    private ListenerRegistration ownersListener;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("villageId")) {
            selectedVillageId = getIntent().getStringExtra("villageId");
            if (selectedVillageId == null || selectedVillageId.isEmpty()) {
                selectedVillageId = "ALL";
            }
        }

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.fabAddOwner.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerListActivity.this, OwnerFormActivity.class);
            if (!"ALL".equals(selectedVillageId)) {
                intent.putExtra("preselectedVillageId", selectedVillageId);
            }
            startActivity(intent);
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            setupListeners();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupRecyclerView() {
        adapter = new OwnerAdapter(new OwnerAdapter.OnOwnerClickListener() {
            @Override
            public void onOwnerClick(Owner owner) {
                Intent intent = new Intent(OwnerListActivity.this, OwnerDetailActivity.class);
                intent.putExtra("owner", owner);
                startActivity(intent);
            }

            @Override
            public void onEditClick(Owner owner) {
                Intent intent = new Intent(OwnerListActivity.this, OwnerFormActivity.class);
                intent.putExtra("owner", owner);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Owner owner) {
                confirmDeleteOwner(owner);
            }
        });

        binding.rvOwners.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOwners.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearchOwner.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    binding.btnClearSearch.setVisibility(View.GONE);
                    filterOwners();
                } else {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                    filterOwners();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearchOwner.setText(""));
    }

    private void setupListeners() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Villages
        if (villagesListener != null) villagesListener.remove();
        villagesListener = repository.listenToVillages(new FirebaseRepository.DataCallback<List<Village>>() {
            @Override
            public void onSuccess(List<Village> data) {
                villageList = data != null ? data : new ArrayList<>();
                setupVillageSpinner();
            }

            @Override
            public void onError(Exception e) {
            }
        });

        // Owners
        if (ownersListener != null) ownersListener.remove();
        ownersListener = repository.listenToOwners(null, new FirebaseRepository.DataCallback<List<Owner>>() {
            @Override
            public void onSuccess(List<Owner> data) {
                binding.progressBar.setVisibility(View.GONE);
                allOwners = data != null ? data : new ArrayList<>();
                updateCountsAndFilter();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OwnerListActivity.this, "Error loading owners: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Surveys (to calculate animal counts dynamically)
        if (surveysListener != null) surveysListener.remove();
        surveysListener = repository.listenToSurveys(null, new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                allSurveys = data != null ? data : new ArrayList<>();
                updateCountsAndFilter();
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void setupVillageSpinner() {
        List<String> spinnerLabels = new ArrayList<>();
        spinnerLabels.add("All Villages");
        int selectIndex = 0;

        for (int i = 0; i < villageList.size(); i++) {
            Village v = villageList.get(i);
            spinnerLabels.add(v.getName());
            if (v.getId().equals(selectedVillageId)) {
                selectIndex = i + 1;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilterVillage.setAdapter(adapter);

        if (selectIndex > 0) {
            binding.spinnerFilterVillage.setSelection(selectIndex);
        }

        binding.spinnerFilterVillage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedVillageId = "ALL";
                } else {
                    selectedVillageId = villageList.get(position - 1).getId();
                }
                filterOwners();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateCountsAndFilter() {
        // Compute animal counts per owner
        Map<String, Integer> counts = new HashMap<>();
        for (AnimalSurvey s : allSurveys) {
            if (s.getOwnerId() != null && !s.getOwnerId().isEmpty()) {
                int current = counts.getOrDefault(s.getOwnerId(), 0);
                counts.put(s.getOwnerId(), current + s.getQuantity());
            }
        }
        adapter.setOwnerAnimalCounts(counts);
        filterOwners();
    }

    private void filterOwners() {
        String query = binding.etSearchOwner.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Owner> filtered = new ArrayList<>();

        for (Owner o : allOwners) {
            boolean villageMatch = "ALL".equals(selectedVillageId) || (o.getVillageId() != null && o.getVillageId().equals(selectedVillageId));
            boolean nameMatch = query.isEmpty() || o.getName().toLowerCase(Locale.ROOT).contains(query);

            if (villageMatch && nameMatch) {
                filtered.add(o);
            }
        }

        adapter.setOwners(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvOwners.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvOwners.setVisibility(View.VISIBLE);
        }
    }

    private void confirmDeleteOwner(Owner owner) {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.checkOwnerCanDelete(owner.getId(), new FirebaseRepository.DeleteSafetyCallback() {
            @Override
            public void onResult(boolean canDeleteSafely, int associatedOwners, int associatedSurveys) {
                binding.progressBar.setVisibility(View.GONE);
                if (!canDeleteSafely) {
                    new AlertDialog.Builder(OwnerListActivity.this)
                            .setTitle("Cannot Delete Owner")
                            .setMessage("Owner '" + owner.getName() + "' has " + associatedSurveys +
                                    " animal survey records registered.\n\nPlease delete the associated survey records first to keep data accurate.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(OwnerListActivity.this)
                            .setTitle("Delete Owner")
                            .setMessage("Are you sure you want to permanently delete '" + owner.getName() + "'?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                binding.progressBar.setVisibility(View.VISIBLE);
                                repository.deleteOwner(owner.getId(), new FirebaseRepository.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        binding.progressBar.setVisibility(View.GONE);
                                        Toast.makeText(OwnerListActivity.this, "Owner deleted successfully", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        binding.progressBar.setVisibility(View.GONE);
                                        Toast.makeText(OwnerListActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(OwnerListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
        if (ownersListener != null) ownersListener.remove();
        if (surveysListener != null) surveysListener.remove();
    }
}
