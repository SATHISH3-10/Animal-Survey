package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.adapters.SurveyAdapter;
import com.example.vilage_wise.databinding.ActivitySurveyListBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.Constants;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SurveyListActivity extends AppCompatActivity {

    private ActivitySurveyListBinding binding;
    private FirebaseRepository repository;
    private SurveyAdapter adapter;

    private List<Village> villageList = new ArrayList<>();
    private List<AnimalSurvey> allSurveys = new ArrayList<>();

    private String selectedVillageId = "ALL";
    private String selectedAnimalType = "ALL";

    private ListenerRegistration villagesListener;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySurveyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("villageId")) {
            selectedVillageId = getIntent().getStringExtra("villageId");
            if (selectedVillageId == null || selectedVillageId.isEmpty()) {
                selectedVillageId = "ALL";
            }
        }

        if (getIntent().hasExtra("animalType")) {
            selectedAnimalType = getIntent().getStringExtra("animalType");
            if (selectedAnimalType == null || selectedAnimalType.isEmpty()) {
                selectedAnimalType = "ALL";
            }
        }

        setupToolbar();
        setupRecyclerView();
        setupAnimalFilterSpinner();
        setupSearch();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.fabAddSurvey.setOnClickListener(v -> {
            Intent intent = new Intent(SurveyListActivity.this, SurveyFormActivity.class);
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
        adapter = new SurveyAdapter(survey -> {
            Intent intent = new Intent(SurveyListActivity.this, SurveyDetailActivity.class);
            intent.putExtra("survey", survey);
            startActivity(intent);
        });

        binding.rvSurveys.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSurveys.setAdapter(adapter);
    }

    private void setupAnimalFilterSpinner() {
        List<String> animalFilters = new ArrayList<>();
        animalFilters.add("All Animals");
        for (String type : Constants.ALL_COMMON_TYPES) {
            animalFilters.add(type);
        }

        ArrayAdapter<String> animalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalFilters);
        animalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilterAnimal.setAdapter(animalAdapter);

        int selectIdx = 0;
        if (!"ALL".equalsIgnoreCase(selectedAnimalType)) {
            for (int i = 0; i < animalFilters.size(); i++) {
                if (animalFilters.get(i).equalsIgnoreCase(selectedAnimalType)) {
                    selectIdx = i;
                    break;
                }
            }
        }
        binding.spinnerFilterAnimal.setSelection(selectIdx);

        binding.spinnerFilterAnimal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedAnimalType = "ALL";
                } else {
                    selectedAnimalType = animalFilters.get(position);
                }
                filterSurveys();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSearch() {
        binding.etSearchSurvey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    binding.btnClearSearch.setVisibility(View.GONE);
                } else {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                }
                filterSurveys();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearchSurvey.setText(""));
    }

    private void setupListeners() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Villages for spinner
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

        // Surveys
        if (surveysListener != null) surveysListener.remove();
        surveysListener = repository.listenToSurveys(null, new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                binding.progressBar.setVisibility(View.GONE);
                allSurveys = data != null ? data : new ArrayList<>();
                filterSurveys();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(SurveyListActivity.this, "Error loading surveys: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                filterSurveys();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void filterSurveys() {
        String query = binding.etSearchSurvey.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<AnimalSurvey> filtered = new ArrayList<>();

        for (AnimalSurvey s : allSurveys) {
            boolean villageMatch = "ALL".equals(selectedVillageId) || (s.getVillageId() != null && s.getVillageId().equals(selectedVillageId));

            boolean animalMatch = "ALL".equalsIgnoreCase(selectedAnimalType) ||
                    (s.getDisplayAnimalType() != null && s.getDisplayAnimalType().toLowerCase(Locale.ROOT).contains(selectedAnimalType.toLowerCase(Locale.ROOT))) ||
                    (s.getAnimalCategory() != null && s.getAnimalCategory().equalsIgnoreCase(selectedAnimalType));

            boolean searchMatch = query.isEmpty() ||
                    (s.getOwnerName() != null && s.getOwnerName().toLowerCase(Locale.ROOT).contains(query)) ||
                    (s.getBreed() != null && s.getBreed().toLowerCase(Locale.ROOT).contains(query)) ||
                    (s.getDisplayAnimalType() != null && s.getDisplayAnimalType().toLowerCase(Locale.ROOT).contains(query));

            if (villageMatch && animalMatch && searchMatch) {
                filtered.add(s);
            }
        }

        adapter.setSurveys(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvSurveys.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvSurveys.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
        if (surveysListener != null) surveysListener.remove();
    }
}
