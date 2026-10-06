package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.adapters.GroupedOwnerAdapter;
import com.example.vilage_wise.databinding.ActivityAnimalGroupedOwnersBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.OwnerAnimalCount;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnimalGroupedOwnersActivity extends AppCompatActivity {

    private ActivityAnimalGroupedOwnersBinding binding;
    private FirebaseRepository repository;
    private GroupedOwnerAdapter adapter;

    private String animalType = "Dogs";
    private String title = "Dogs";
    private String villageId = "ALL";
    private String villageName = "All Villages";

    private List<AnimalSurvey> allSurveys = new ArrayList<>();
    private List<OwnerAnimalCount> groupedList = new ArrayList<>();
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnimalGroupedOwnersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("animalType")) {
            animalType = getIntent().getStringExtra("animalType");
        }
        if (getIntent().hasExtra("title")) {
            title = getIntent().getStringExtra("title");
        }
        if (getIntent().hasExtra("villageId")) {
            villageId = getIntent().getStringExtra("villageId");
        }
        if (getIntent().hasExtra("villageName")) {
            villageName = getIntent().getStringExtra("villageName");
        }

        setupUI();
        setupSearch();
        setupListeners();
    }

    private void setupUI() {
        binding.toolbar.setTitle(title + " — Owner Distribution");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.tvAnimalTypeHeader.setText(title + " Survey Breakdown");
        binding.tvVillageFilterHeader.setText("Village: " + villageName);

        adapter = new GroupedOwnerAdapter(item -> {
            // View Owner Profile or Surveys for this owner
            if (item.getOwnerId() != null && !item.getOwnerId().startsWith("unknown_")) {
                repository.getOwnerById(item.getOwnerId(), new FirebaseRepository.DataCallback<Owner>() {
                    @Override
                    public void onSuccess(Owner owner) {
                        if (owner != null) {
                            Intent intent = new Intent(AnimalGroupedOwnersActivity.this, OwnerDetailActivity.class);
                            intent.putExtra("owner", owner);
                            startActivity(intent);
                        } else {
                            openOwnerSurveys(item);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        openOwnerSurveys(item);
                    }
                });
            } else {
                openOwnerSurveys(item);
            }
        });

        binding.rvGroupedOwners.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGroupedOwners.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> {
            setupListeners();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void openOwnerSurveys(OwnerAnimalCount item) {
        Intent intent = new Intent(AnimalGroupedOwnersActivity.this, SurveyListActivity.class);
        if (!"ALL".equals(villageId)) {
            intent.putExtra("villageId", villageId);
        }
        intent.putExtra("animalType", animalType);
        startActivity(intent);
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
                    adapter.setCountList(groupedList);
                    updateEmptyState(groupedList.isEmpty());
                } else {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                    filterGroupedList(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearchOwner.setText(""));
    }

    private void filterGroupedList(String query) {
        List<OwnerAnimalCount> filtered = new ArrayList<>();
        for (OwnerAnimalCount item : groupedList) {
            if (item.getOwnerName().toLowerCase(Locale.ROOT).contains(query) ||
                    item.getVillageName().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(item);
            }
        }
        adapter.setCountList(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void setupListeners() {
        binding.progressBar.setVisibility(View.VISIBLE);
        if (surveysListener != null) surveysListener.remove();

        String villageFilter = "ALL".equals(villageId) ? null : villageId;

        surveysListener = repository.listenToSurveys(villageFilter, new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                binding.progressBar.setVisibility(View.GONE);
                allSurveys = data != null ? data : new ArrayList<>();
                recalculate();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AnimalGroupedOwnersActivity.this, "Error loading animal distribution: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recalculate() {
        groupedList = FirebaseRepository.groupSurveysByOwnerForAnimal(allSurveys, animalType);

        int totalAnimalSum = 0;
        for (OwnerAnimalCount item : groupedList) {
            totalAnimalSum += item.getTotalCount();
        }

        binding.tvTotalAnimalCount.setText(String.valueOf(totalAnimalSum));

        String query = binding.etSearchOwner.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            adapter.setCountList(groupedList);
            updateEmptyState(groupedList.isEmpty());
        } else {
            filterGroupedList(query);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvGroupedOwners.setVisibility(View.GONE);
            binding.tvEmptySubtitle.setText("No survey records found for " + title + " in " + villageName + ".");
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvGroupedOwners.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surveysListener != null) surveysListener.remove();
    }
}
