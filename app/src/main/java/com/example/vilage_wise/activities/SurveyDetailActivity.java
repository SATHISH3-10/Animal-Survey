package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.databinding.ActivitySurveyDetailBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.DateUtils;

public class SurveyDetailActivity extends AppCompatActivity {

    private ActivitySurveyDetailBinding binding;
    private FirebaseRepository repository;
    private AnimalSurvey survey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySurveyDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("survey")) {
            survey = (AnimalSurvey) getIntent().getSerializableExtra("survey");
        }

        if (survey == null) {
            Toast.makeText(this, "Survey data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        populateDetails();

        binding.btnEditSurvey.setOnClickListener(v -> {
            Intent intent = new Intent(SurveyDetailActivity.this, SurveyFormActivity.class);
            intent.putExtra("survey", survey);
            startActivity(intent);
        });

        binding.btnDeleteSurvey.setOnClickListener(v -> confirmDelete());
    }

    private void populateDetails() {
        binding.tvAnimalType.setText(survey.getDisplayAnimalType());
        binding.tvCategoryBadge.setText(survey.getAnimalCategory());
        binding.tvQuantity.setText(String.valueOf(survey.getQuantity()));

        binding.tvOwnerName.setText(survey.getOwnerName() != null ? survey.getOwnerName() : "Unknown Owner");
        binding.tvVillageName.setText(survey.getVillageName() != null ? survey.getVillageName() : "Unknown Village");

        // Format detailed location breakdown
        StringBuilder addressBuilder = new StringBuilder();
        if (survey.getLocality() != null && !survey.getLocality().trim().isEmpty()) {
            addressBuilder.append(survey.getLocality().trim()).append(", ");
        }
        if (survey.getDistrict() != null && !survey.getDistrict().trim().isEmpty()) {
            addressBuilder.append(survey.getDistrict().trim()).append(", ");
        }
        if (survey.getState() != null && !survey.getState().trim().isEmpty()) {
            addressBuilder.append(survey.getState().trim());
        }
        if (survey.getPincode() != null && !survey.getPincode().trim().isEmpty()) {
            addressBuilder.append(" - ").append(survey.getPincode().trim());
        }
        if (survey.getCountry() != null && !survey.getCountry().trim().isEmpty() && !"India".equalsIgnoreCase(survey.getCountry().trim())) {
            addressBuilder.append(" (").append(survey.getCountry().trim()).append(")");
        }

        String fullAddress = addressBuilder.toString();
        if (!fullAddress.isEmpty()) {
            binding.tvFullAddress.setText(fullAddress);
            binding.tvFullAddress.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.tvFullAddress.setVisibility(android.view.View.GONE);
        }

        // Coordinates & Source Badge
        if ("GPS".equalsIgnoreCase(survey.getLocationSource())) {
            binding.tvLocationSourceBadge.setText("GPS Detected");
            binding.tvLocationSourceBadge.setBackgroundResource(com.example.vilage_wise.R.drawable.bg_badge_gps);
            binding.tvLocationSourceBadge.setTextColor(0xFF2E7D32);
        } else {
            binding.tvLocationSourceBadge.setText("Manual Entry");
            binding.tvLocationSourceBadge.setBackgroundResource(com.example.vilage_wise.R.drawable.bg_badge_manual);
            binding.tvLocationSourceBadge.setTextColor(0xFFE65100);
        }

        if (survey.getLatitude() != 0.0 || survey.getLongitude() != 0.0) {
            binding.tvCoordinates.setText(String.format(java.util.Locale.US, "GPS: %.5f, %.5f", survey.getLatitude(), survey.getLongitude()));
            binding.tvCoordinates.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.tvCoordinates.setVisibility(android.view.View.GONE);
        }
        if (survey.getBreed() != null && !survey.getBreed().trim().isEmpty()) {
            binding.tvBreed.setText(survey.getBreed().trim());
        } else {
            binding.tvBreed.setText("Not specified");
        }

        if (survey.getAge() != null && !survey.getAge().trim().isEmpty()) {
            binding.tvAge.setText(survey.getAge().trim());
        } else {
            binding.tvAge.setText("Not specified");
        }

        if (survey.getGender() != null && !survey.getGender().trim().isEmpty()) {
            binding.tvGender.setText(survey.getGender().trim());
        } else {
            binding.tvGender.setText("Not specified");
        }

        binding.tvSurveyDate.setText(DateUtils.formatDateTime(survey.getSurveyDate()));

        if (survey.getNotes() != null && !survey.getNotes().trim().isEmpty()) {
            binding.tvNotes.setText(survey.getNotes().trim());
        } else {
            binding.tvNotes.setText("No additional notes recorded");
        }

        String surveyorName = survey.getSurveyorName();
        String surveyorEmail = survey.getSurveyorEmail();
        if (surveyorName != null && !surveyorName.trim().isEmpty()) {
            String text = surveyorName;
            if (surveyorEmail != null && !surveyorEmail.trim().isEmpty()) {
                text += " (" + surveyorEmail + ")";
            }
            binding.tvSurveyorOfficer.setText(text);
        } else {
            binding.tvSurveyorOfficer.setText("Assigned Field Officer");
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Survey Record")
                .setMessage("Are you sure you want to permanently delete this animal survey record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteSurvey(survey.getId(), new FirebaseRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(SurveyDetailActivity.this, "Survey record deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(SurveyDetailActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload survey from Firestore if updated
        if (survey != null && survey.getId() != null) {
            repository.listenToSurveys(survey.getVillageId(), new FirebaseRepository.DataCallback<java.util.List<AnimalSurvey>>() {
                @Override
                public void onSuccess(java.util.List<AnimalSurvey> data) {
                    if (data != null) {
                        for (AnimalSurvey s : data) {
                            if (s.getId().equals(survey.getId())) {
                                survey = s;
                                populateDetails();
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                }
            });
        }
    }
}
