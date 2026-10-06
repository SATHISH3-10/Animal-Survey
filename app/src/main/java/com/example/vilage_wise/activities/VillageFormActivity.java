package com.example.vilage_wise.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.R;
import com.example.vilage_wise.databinding.ActivityVillageFormBinding;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.LocationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class VillageFormActivity extends AppCompatActivity {

    private ActivityVillageFormBinding binding;
    private FirebaseRepository repository;
    private Village existingVillage;
    private boolean isEditMode = false;
    private String currentLocationSource = "MANUAL";
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION));

                if (fineGranted || coarseGranted) {
                    performLocationDetection();
                } else {
                    Toast.makeText(this, "Location permission denied. You can still enter village details manually.", Toast.LENGTH_LONG).show();
                    setManualModeUI();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVillageFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("village")) {
            existingVillage = (Village) getIntent().getSerializableExtra("village");
            if (existingVillage != null) {
                isEditMode = true;
            }
        }

        setupUI();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (isEditMode) {
            binding.toolbar.setTitle("Edit Village");
            binding.tvFormTitle.setText("Edit Village Information");
            binding.etVillageName.setText(existingVillage.getName());
            binding.etLocality.setText(existingVillage.getLocality());
            binding.etPincode.setText(existingVillage.getPincode());
            binding.etDistrict.setText(existingVillage.getDistrict());
            binding.etState.setText(existingVillage.getState());
            binding.etCountry.setText(existingVillage.getCountry());
            binding.etVillageCode.setText(existingVillage.getCode());
            if (existingVillage.getLatitude() != 0.0 || existingVillage.getLongitude() != 0.0) {
                binding.etLatitude.setText(String.format(Locale.US, "%.6f", existingVillage.getLatitude()));
                binding.etLongitude.setText(String.format(Locale.US, "%.6f", existingVillage.getLongitude()));
                currentLatitude = existingVillage.getLatitude();
                currentLongitude = existingVillage.getLongitude();
            }
            currentLocationSource = existingVillage.getLocationSource();
            updateSourceBadge();
            binding.btnSaveVillage.setText("Update Village");
        } else {
            binding.toolbar.setTitle("Add New Village");
            binding.tvFormTitle.setText("Add New Village");
            binding.btnSaveVillage.setText("Save Village");
            setManualModeUI();
        }

        binding.btnUseCurrentLocation.setOnClickListener(v -> handleUseCurrentLocationClicked());
        binding.btnEnterManually.setOnClickListener(v -> setManualModeUI());
        binding.btnSaveVillage.setOnClickListener(v -> validateAndSave());
    }

    private void handleUseCurrentLocationClicked() {
        String existingName = binding.etVillageName.getText() != null ? binding.etVillageName.getText().toString().trim() : "";
        if (!existingName.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Detect Current Location?")
                    .setMessage("Automatically detecting your location may overwrite the village and district details you have entered. Do you want to proceed?")
                    .setPositiveButton("Detect GPS", (dialog, which) -> checkPermissionsAndDetect())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            checkPermissionsAndDetect();
        }
    }

    private void checkPermissionsAndDetect() {
        if (!LocationHelper.hasLocationPermission(this)) {
            locationPermissionLauncher.launch(LocationHelper.LOCATION_PERMISSIONS);
        } else {
            performLocationDetection();
        }
    }

    private void performLocationDetection() {
        binding.layoutLocationLoading.setVisibility(View.VISIBLE);
        binding.tvLocationLoadingStatus.setText("Acquiring GPS coordinates & address details...");
        binding.btnUseCurrentLocation.setEnabled(false);

        LocationHelper.fetchCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onSuccess(LocationHelper.LocationResult result) {
                binding.layoutLocationLoading.setVisibility(View.GONE);
                binding.btnUseCurrentLocation.setEnabled(true);

                currentLatitude = result.latitude;
                currentLongitude = result.longitude;
                currentLocationSource = "GPS";

                if (result.latitude != 0.0) {
                    binding.etLatitude.setText(String.format(Locale.US, "%.6f", result.latitude));
                }
                if (result.longitude != 0.0) {
                    binding.etLongitude.setText(String.format(Locale.US, "%.6f", result.longitude));
                }

                if (result.geocodingSucceeded) {
                    if (!result.villageOrLocality.isEmpty()) {
                        binding.etVillageName.setText(result.villageOrLocality);
                    }
                    if (!result.locality.isEmpty()) {
                        binding.etLocality.setText(result.locality);
                    }
                    if (!result.pincode.isEmpty()) {
                        binding.etPincode.setText(result.pincode);
                    }
                    if (!result.district.isEmpty()) {
                        binding.etDistrict.setText(result.district);
                    }
                    if (!result.state.isEmpty()) {
                        binding.etState.setText(result.state);
                    }
                    if (!result.country.isEmpty()) {
                        binding.etCountry.setText(result.country);
                    }
                    Toast.makeText(VillageFormActivity.this, "Location detected! Please review the details before saving.", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = (result.note != null && !result.note.isEmpty())
                            ? result.note
                            : "GPS coordinates captured. Please fill in the village name manually.";
                    Toast.makeText(VillageFormActivity.this, msg, Toast.LENGTH_LONG).show();
                }

                updateSourceBadge();
            }

            @Override
            public void onError(String errorMessage) {
                binding.layoutLocationLoading.setVisibility(View.GONE);
                binding.btnUseCurrentLocation.setEnabled(true);
                new MaterialAlertDialogBuilder(VillageFormActivity.this)
                        .setTitle("Location Detection")
                        .setMessage(errorMessage + "\n\nYou can continue entering the village information manually.")
                        .setPositiveButton("Enter Manually", (dialog, which) -> setManualModeUI())
                        .setNegativeButton("Retry GPS", (dialog, which) -> performLocationDetection())
                        .show();
            }
        });
    }

    private void setManualModeUI() {
        currentLocationSource = "MANUAL";
        updateSourceBadge();
        binding.etVillageName.requestFocus();
    }

    private void updateSourceBadge() {
        if ("GPS".equalsIgnoreCase(currentLocationSource)) {
            binding.tvSourcePill.setText("GPS Detected");
            binding.tvSourcePill.setBackgroundResource(R.drawable.bg_badge_gps);
            binding.tvSourcePill.setTextColor(0xFF2E7D32); // Green
            if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                binding.tvCoordinatesDisplay.setText(String.format(Locale.US, "Lat: %.4f, Lng: %.4f", currentLatitude, currentLongitude));
            } else {
                binding.tvCoordinatesDisplay.setText("GPS active");
            }
        } else {
            binding.tvSourcePill.setText("Manual Entry Mode");
            binding.tvSourcePill.setBackgroundResource(R.drawable.bg_badge_manual);
            binding.tvSourcePill.setTextColor(0xFFE65100); // Orange
            if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                binding.tvCoordinatesDisplay.setText(String.format(Locale.US, "Coords: %.4f, %.4f", currentLatitude, currentLongitude));
            } else {
                binding.tvCoordinatesDisplay.setText("GPS not required");
            }
        }
    }

    private void validateAndSave() {
        String name = binding.etVillageName.getText() != null ? binding.etVillageName.getText().toString().trim() : "";
        String locality = binding.etLocality.getText() != null ? binding.etLocality.getText().toString().trim() : "";
        String pincode = binding.etPincode.getText() != null ? binding.etPincode.getText().toString().trim() : "";
        String district = binding.etDistrict.getText() != null ? binding.etDistrict.getText().toString().trim() : "";
        String state = binding.etState.getText() != null ? binding.etState.getText().toString().trim() : "";
        String country = binding.etCountry.getText() != null ? binding.etCountry.getText().toString().trim() : "India";
        String code = binding.etVillageCode.getText() != null ? binding.etVillageCode.getText().toString().trim() : "";

        String latStr = binding.etLatitude.getText() != null ? binding.etLatitude.getText().toString().trim() : "";
        String lngStr = binding.etLongitude.getText() != null ? binding.etLongitude.getText().toString().trim() : "";

        double lat = currentLatitude;
        double lng = currentLongitude;
        try {
            if (!latStr.isEmpty()) lat = Double.parseDouble(latStr);
            if (!lngStr.isEmpty()) lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException ignored) {
        }

        if (name.isEmpty()) {
            binding.tilVillageName.setError("Village name is required");
            binding.etVillageName.requestFocus();
            binding.scrollView.smoothScrollTo(0, binding.tilVillageName.getTop());
            return;
        } else {
            binding.tilVillageName.setError(null);
        }

        if (district.isEmpty()) {
            binding.tilDistrict.setError("District is required");
            binding.etDistrict.requestFocus();
            binding.scrollView.smoothScrollTo(0, binding.tilDistrict.getTop());
            return;
        } else {
            binding.tilDistrict.setError(null);
        }

        if (state.isEmpty()) {
            binding.tilState.setError("State is required");
            binding.etState.requestFocus();
            binding.scrollView.smoothScrollTo(0, binding.tilState.getTop());
            return;
        } else {
            binding.tilState.setError(null);
        }

        if (country.isEmpty()) {
            country = "India";
        }

        // Validate Indian 6-digit PIN code if country is India and PIN is entered
        if ("India".equalsIgnoreCase(country) && !pincode.isEmpty()) {
            if (!LocationHelper.isValidIndianPincode(pincode)) {
                binding.tilPincode.setError("Please enter a valid 6-digit Indian PIN code");
                binding.etPincode.requestFocus();
                binding.scrollView.smoothScrollTo(0, binding.tilPincode.getTop());
                return;
            } else {
                binding.tilPincode.setError(null);
            }
        } else {
            binding.tilPincode.setError(null);
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveVillage.setEnabled(false);

        String excludeId = isEditMode ? existingVillage.getId() : "";

        final String finalCountry = country;
        final double finalLat = lat;
        final double finalLng = lng;

        // Check for duplicates
        repository.checkVillageDuplicate(name, code, excludeId, new FirebaseRepository.DuplicateCheckCallback() {
            @Override
            public void onResult(boolean isDuplicate, String message) {
                if (isDuplicate) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveVillage.setEnabled(true);
                    Toast.makeText(VillageFormActivity.this, message, Toast.LENGTH_LONG).show();
                    binding.tilVillageName.setError(message);
                } else {
                    saveVillageRecord(name, locality, pincode, district, state, finalCountry, code, finalLat, finalLng);
                }
            }

            @Override
            public void onError(Exception e) {
                // If duplicate check fails due to offline/network, proceed with saving
                saveVillageRecord(name, locality, pincode, district, state, finalCountry, code, finalLat, finalLng);
            }
        });
    }

    private void saveVillageRecord(String name, String locality, String pincode, String district, 
                                   String state, String country, String code, double lat, double lng) {
        if (isEditMode) {
            existingVillage.setName(name);
            existingVillage.setLocality(locality);
            existingVillage.setPincode(pincode);
            existingVillage.setDistrict(district);
            existingVillage.setState(state);
            existingVillage.setCountry(country);
            existingVillage.setCode(code);
            existingVillage.setLatitude(lat);
            existingVillage.setLongitude(lng);
            existingVillage.setLocationSource(currentLocationSource);

            repository.updateVillage(existingVillage, new FirebaseRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(VillageFormActivity.this, "Village updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveVillage.setEnabled(true);
                    Toast.makeText(VillageFormActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Village newVillage = new Village(
                    "",
                    name,
                    locality,
                    pincode,
                    district,
                    state,
                    country,
                    code,
                    lat,
                    lng,
                    currentLocationSource,
                    System.currentTimeMillis()
            );
            repository.addVillage(newVillage, new FirebaseRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(VillageFormActivity.this, "Village added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveVillage.setEnabled(true);
                    Toast.makeText(VillageFormActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
