package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.R;
import com.example.vilage_wise.databinding.ActivityVillageStatisticsBinding;
import com.example.vilage_wise.models.AnimalStats;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VillageStatisticsActivity extends AppCompatActivity {

    private ActivityVillageStatisticsBinding binding;
    private FirebaseRepository repository;

    private String selectedVillageId = "ALL";
    private String selectedVillageName = "All Villages";

    private List<Village> villageList = new ArrayList<>();
    private List<Owner> allOwners = new ArrayList<>();
    private List<AnimalSurvey> allSurveys = new ArrayList<>();

    private ListenerRegistration villagesListener;
    private ListenerRegistration ownersListener;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVillageStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("villageId")) {
            selectedVillageId = getIntent().getStringExtra("villageId");
            if (selectedVillageId == null || selectedVillageId.isEmpty()) {
                selectedVillageId = "ALL";
            }
        }
        if (getIntent().hasExtra("villageName")) {
            selectedVillageName = getIntent().getStringExtra("villageName");
            if (selectedVillageName == null || selectedVillageName.isEmpty()) {
                selectedVillageName = "All Villages";
            }
        }

        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.swipeRefresh.setOnRefreshListener(() -> {
            setupListeners();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupListeners() {
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
                allOwners = data != null ? data : new ArrayList<>();
                recalculate();
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
                allSurveys = data != null ? data : new ArrayList<>();
                recalculate();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(VillageStatisticsActivity.this, "Error syncing statistics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupVillageSpinner() {
        List<String> spinnerLabels = new ArrayList<>();
        spinnerLabels.add("All Villages (Overview)");
        int selectIndex = 0;

        for (int i = 0; i < villageList.size(); i++) {
            Village v = villageList.get(i);
            spinnerLabels.add(v.getName() + " (" + v.getDistrict() + ")");
            if (v.getId().equals(selectedVillageId)) {
                selectIndex = i + 1;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerVillage.setAdapter(adapter);

        if (selectIndex > 0) {
            binding.spinnerVillage.setSelection(selectIndex);
        }

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
        List<AnimalSurvey> filteredSurveys = new ArrayList<>();
        List<Owner> filteredOwners = new ArrayList<>();

        for (AnimalSurvey s : allSurveys) {
            if ("ALL".equals(selectedVillageId) || (s.getVillageId() != null && s.getVillageId().equals(selectedVillageId))) {
                filteredSurveys.add(s);
            }
        }

        for (Owner o : allOwners) {
            if ("ALL".equals(selectedVillageId) || (o.getVillageId() != null && o.getVillageId().equals(selectedVillageId))) {
                filteredOwners.add(o);
            }
        }

        AnimalStats stats = FirebaseRepository.calculateStats(filteredSurveys, filteredOwners);

        binding.tvStatsVillageName.setText(selectedVillageName);
        if ("ALL".equals(selectedVillageId)) {
            binding.tvStatsSubtitle.setText("Aggregated statistics across " + villageList.size() + " active villages");
        } else {
            binding.tvStatsSubtitle.setText("Detailed livestock and pet census");
        }

        binding.tvStatTotalAnimals.setText(String.valueOf(stats.getTotalAnimals()));
        binding.tvStatTotalOwners.setText(String.valueOf(filteredOwners.size()));
        binding.tvStatTotalSurveys.setText(String.valueOf(filteredSurveys.size()));

        // Populate breakdown rows
        populateAnimalBreakdownRows(stats, stats.getTotalAnimals());
    }

    private void populateAnimalBreakdownRows(AnimalStats stats, int totalAnimals) {
        binding.layoutAnimalRows.removeAllViews();

        Map<String, Integer> counts = stats.getDetailedTypeCounts();
        if (counts.isEmpty() || totalAnimals == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No animal records available to display statistics.");
            tvEmpty.setTextColor(getColor(R.color.text_hint));
            tvEmpty.setPadding(0, 16, 0, 16);
            binding.layoutAnimalRows.addView(tvEmpty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            if (count <= 0) continue;

            int percentage = (int) Math.round(((double) count / (double) totalAnimals) * 100.0);

            View rowView = inflater.inflate(R.layout.item_stat_row, binding.layoutAnimalRows, false);
            TextView tvTypeName = rowView.findViewById(R.id.tvStatTypeName);
            TextView tvCount = rowView.findViewById(R.id.tvStatCount);
            TextView tvPercentage = rowView.findViewById(R.id.tvStatPercentage);
            ProgressBar progress = rowView.findViewById(R.id.progressStat);
            ImageView ivChevron = rowView.findViewById(R.id.ivChevron);

            tvTypeName.setText(getEmojiForType(type) + " " + type);
            tvCount.setText(count + " animals");
            tvPercentage.setText(percentage + "%");
            progress.setProgress(percentage);

            rowView.setOnClickListener(v -> {
                Intent intent = new Intent(VillageStatisticsActivity.this, AnimalGroupedOwnersActivity.class);
                intent.putExtra("animalType", type);
                intent.putExtra("title", type);
                intent.putExtra("villageId", selectedVillageId);
                intent.putExtra("villageName", selectedVillageName);
                startActivity(intent);
            });

            binding.layoutAnimalRows.addView(rowView);
        }
    }

    private String getEmojiForType(String type) {
        String lower = type.toLowerCase(Locale.ROOT);
        if (lower.contains("cow")) return "🐄";
        if (lower.contains("buffalo")) return "🐃";
        if (lower.contains("goat")) return "🐐";
        if (lower.contains("sheep")) return "🐑";
        if (lower.contains("pig")) return "🐖";
        if (lower.contains("dog")) return "🐕";
        if (lower.contains("cat")) return "🐈";
        if (lower.contains("bird")) return "🦜";
        if (lower.contains("chicken")) return "🐓";
        if (lower.contains("duck")) return "🦆";
        return "🐾";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
        if (ownersListener != null) ownersListener.remove();
        if (surveysListener != null) surveysListener.remove();
    }
}
