package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vilage_wise.adapters.SurveyAdapter;
import com.example.vilage_wise.databinding.ActivityOwnerDetailBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class OwnerDetailActivity extends AppCompatActivity {

    private ActivityOwnerDetailBinding binding;
    private FirebaseRepository repository;
    private Owner owner;
    private SurveyAdapter surveyAdapter;
    private ListenerRegistration surveysListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("owner")) {
            owner = (Owner) getIntent().getSerializableExtra("owner");
        }

        if (owner == null) {
            Toast.makeText(this, "Owner information missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadOwnerSurveys();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        populateOwnerInfo();

        surveyAdapter = new SurveyAdapter(survey -> {
            Intent intent = new Intent(OwnerDetailActivity.this, SurveyDetailActivity.class);
            intent.putExtra("survey", survey);
            startActivity(intent);
        });

        binding.rvOwnerSurveys.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOwnerSurveys.setAdapter(surveyAdapter);

        binding.btnEditOwner.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDetailActivity.this, OwnerFormActivity.class);
            intent.putExtra("owner", owner);
            startActivity(intent);
        });

        binding.btnDeleteOwner.setOnClickListener(v -> confirmDeleteOwner());

        binding.fabAddSurveyForOwner.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDetailActivity.this, SurveyFormActivity.class);
            intent.putExtra("preselectedVillageId", owner.getVillageId());
            intent.putExtra("preselectedOwnerId", owner.getId());
            startActivity(intent);
        });
    }

    private void populateOwnerInfo() {
        binding.tvOwnerName.setText(owner.getName());
        binding.tvVillageName.setText("Village: " + (owner.getVillageName() != null ? owner.getVillageName() : "Assigned"));

        if (owner.getContactNumber() != null && !owner.getContactNumber().trim().isEmpty()) {
            binding.layoutContact.setVisibility(View.VISIBLE);
            binding.tvContactNumber.setText("Phone: " + owner.getContactNumber().trim());
        } else {
            binding.layoutContact.setVisibility(View.GONE);
        }

        if (owner.getAddressLocality() != null && !owner.getAddressLocality().trim().isEmpty()) {
            binding.layoutAddress.setVisibility(View.VISIBLE);
            binding.tvAddress.setText("Address: " + owner.getAddressLocality().trim());
        } else {
            binding.layoutAddress.setVisibility(View.GONE);
        }
    }

    private void loadOwnerSurveys() {
        if (surveysListener != null) surveysListener.remove();

        // Listen for all surveys and filter by owner
        surveysListener = repository.listenToSurveys(null, new FirebaseRepository.DataCallback<List<AnimalSurvey>>() {
            @Override
            public void onSuccess(List<AnimalSurvey> data) {
                List<AnimalSurvey> ownerSurveys = new ArrayList<>();
                int totalAnimals = 0;

                if (data != null) {
                    for (AnimalSurvey s : data) {
                        if (owner.getId().equals(s.getOwnerId())) {
                            ownerSurveys.add(s);
                            totalAnimals += s.getQuantity();
                        }
                    }
                }

                binding.tvTotalAnimalBadge.setText(String.valueOf(totalAnimals));
                binding.tvSurveyCount.setText(ownerSurveys.size() + (ownerSurveys.size() == 1 ? " record" : " records"));

                if (ownerSurveys.isEmpty()) {
                    binding.layoutEmptySurveys.setVisibility(View.VISIBLE);
                    binding.rvOwnerSurveys.setVisibility(View.GONE);
                } else {
                    binding.layoutEmptySurveys.setVisibility(View.GONE);
                    binding.rvOwnerSurveys.setVisibility(View.VISIBLE);
                    surveyAdapter.setSurveys(ownerSurveys);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(OwnerDetailActivity.this, "Error loading animal surveys: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteOwner() {
        repository.checkOwnerCanDelete(owner.getId(), new FirebaseRepository.DeleteSafetyCallback() {
            @Override
            public void onResult(boolean canDeleteSafely, int associatedOwners, int associatedSurveys) {
                if (!canDeleteSafely) {
                    new AlertDialog.Builder(OwnerDetailActivity.this)
                            .setTitle("Cannot Delete Owner")
                            .setMessage("Owner '" + owner.getName() + "' has " + associatedSurveys +
                                    " animal survey records registered.\n\nPlease delete the survey records first to keep statistics accurate.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(OwnerDetailActivity.this)
                            .setTitle("Delete Owner")
                            .setMessage("Are you sure you want to delete '" + owner.getName() + "'?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                repository.deleteOwner(owner.getId(), new FirebaseRepository.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(OwnerDetailActivity.this, "Owner deleted successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(OwnerDetailActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(OwnerDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh owner details from Firestore if edited
        repository.getOwnersByVillage(owner.getVillageId(), new FirebaseRepository.DataCallback<List<Owner>>() {
            @Override
            public void onSuccess(List<Owner> data) {
                if (data != null) {
                    for (Owner o : data) {
                        if (o.getId().equals(owner.getId())) {
                            owner = o;
                            populateOwnerInfo();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surveysListener != null) surveysListener.remove();
    }
}
