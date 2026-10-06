package com.example.vilage_wise.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vilage_wise.R;
import com.example.vilage_wise.databinding.ActivitySurveyFormBinding;
import com.example.vilage_wise.databinding.ItemAnimalSurveyEntryBinding;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.Constants;
import com.example.vilage_wise.utils.DateUtils;
import com.example.vilage_wise.utils.LocationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SurveyFormActivity extends AppCompatActivity {

    private ActivitySurveyFormBinding binding;
    private FirebaseRepository repository;

    private AnimalSurvey existingSurvey;
    private boolean isEditMode = false;

    private String preselectedVillageId = "";
    private String preselectedOwnerId = "";

    private List<Village> villageList = new ArrayList<>();
    private List<Owner> allOwnersList = new ArrayList<>();

    private final List<AnimalEntryHolder> animalHolders = new ArrayList<>();

    private long selectedDateMillis = System.currentTimeMillis();
    private ListenerRegistration villagesListener;
    private ListenerRegistration ownersListener;

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
                    Toast.makeText(this, "Location permission denied. You can enter or select location details manually.", Toast.LENGTH_LONG).show();
                    setManualModeUI();
                }
            });

    private static class AnimalEntryHolder {
        ItemAnimalSurveyEntryBinding itemBinding;
        String selectedCategory = Constants.CATEGORY_LIVESTOCK;
        String selectedType = "Cows";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySurveyFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        // Ensure anonymous login is active in background
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                auth.signInAnonymously();
            }
        } catch (Exception ignored) {
        }

        if (getIntent().hasExtra("survey")) {
            existingSurvey = (AnimalSurvey) getIntent().getSerializableExtra("survey");
            if (existingSurvey != null) {
                isEditMode = true;
            }
        }

        if (getIntent().hasExtra("preselectedVillageId")) {
            preselectedVillageId = getIntent().getStringExtra("preselectedVillageId");
        }

        if (getIntent().hasExtra("preselectedOwnerId")) {
            preselectedOwnerId = getIntent().getStringExtra("preselectedOwnerId");
        }

        setupUI();
        loadData();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Date Picker
        if (isEditMode && existingSurvey != null) {
            selectedDateMillis = existingSurvey.getSurveyDate();
        }
        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDateMillis));
        binding.cardDatePicker.setOnClickListener(v -> showDatePicker());

        // Location Buttons
        binding.btnSurveyUseCurrentLocation.setOnClickListener(v -> handleUseCurrentLocationClicked());
        binding.btnSurveyEnterManually.setOnClickListener(v -> setManualModeUI());

        // Phone number watcher (10 digits auto formatting on paste/type)
        com.example.vilage_wise.utils.PhoneUtils.attachPhoneWatcher(binding.etOwnerContact);

        // Add Animal Button
        binding.btnAddNewAnimal.setOnClickListener(v -> {
            addAnimalEntryView(null);
            // Smoothly scroll down to show the newly added animal card
            binding.scrollView.post(() -> {
                binding.scrollView.smoothScrollTo(0, binding.llAnimalsContainer.getBottom());
            });
        });

        // Edit Mode adjustments
        if (isEditMode && existingSurvey != null) {
            binding.toolbar.setTitle("Edit Survey Record");
            binding.tvFormTitle.setText("Edit Survey Location & Details");
            binding.etVillageName.setText(existingSurvey.getVillageName());
            binding.etLocality.setText(existingSurvey.getLocality());
            binding.etPincode.setText(existingSurvey.getPincode());
            binding.etDistrict.setText(existingSurvey.getDistrict());
            binding.etState.setText(existingSurvey.getState());
            binding.etCountry.setText(existingSurvey.getCountry());
            binding.etOwnerName.setText(existingSurvey.getOwnerName());
            binding.etNotes.setText(existingSurvey.getNotes());
            binding.btnSaveSurvey.setText("Update Survey Record");
            binding.btnAddNewAnimal.setVisibility(View.GONE);

            currentLatitude = existingSurvey.getLatitude();
            currentLongitude = existingSurvey.getLongitude();
            currentLocationSource = existingSurvey.getLocationSource();
            updateSourceBadge();

            // Add single entry prefilled with existing survey
            addAnimalEntryView(existingSurvey);
        } else {
            binding.toolbar.setTitle("New Survey Entry");
            binding.tvFormTitle.setText("1. Survey Location");
            binding.btnSaveSurvey.setText("Save Survey Record");
            setManualModeUI();

            // Add first empty animal entry card
            addAnimalEntryView(null);
        }

        // Keyboard WindowInsets listener
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView, (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(0, 0, 0, imeInsets.bottom);
            return windowInsets;
        });

        // Auto-scroll when notes is focused so notes box & save button are fully visible
        binding.etNotes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.scrollView.postDelayed(() -> {
                    binding.scrollView.smoothScrollTo(0, binding.btnSaveSurvey.getBottom() + 150);
                }, 250);
            }
        });

        binding.btnSaveSurvey.setOnClickListener(v -> validateAndSave());
    }

    private void handleUseCurrentLocationClicked() {
        String existingName = binding.etVillageName.getText() != null ? binding.etVillageName.getText().toString().trim() : "";
        if (!existingName.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Detect Current Location?")
                    .setMessage("Automatically detecting your location may overwrite village and district details. (All owner and animal records entered below will NOT be affected). Proceed?")
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
        binding.layoutSurveyLocationLoading.setVisibility(View.VISIBLE);
        binding.tvSurveyLocationLoadingStatus.setText("Acquiring GPS coordinates & address details...");
        binding.btnSurveyUseCurrentLocation.setEnabled(false);

        LocationHelper.fetchCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onSuccess(LocationHelper.LocationResult result) {
                binding.layoutSurveyLocationLoading.setVisibility(View.GONE);
                binding.btnSurveyUseCurrentLocation.setEnabled(true);

                currentLatitude = result.latitude;
                currentLongitude = result.longitude;
                currentLocationSource = "GPS";

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
                    Toast.makeText(SurveyFormActivity.this, "Location detected! Please review and continue.", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = (result.note != null && !result.note.isEmpty())
                            ? result.note
                            : "GPS coordinates captured. Please enter the village name manually.";
                    Toast.makeText(SurveyFormActivity.this, msg, Toast.LENGTH_LONG).show();
                }

                updateSourceBadge();
            }

            @Override
            public void onError(String errorMessage) {
                binding.layoutSurveyLocationLoading.setVisibility(View.GONE);
                binding.btnSurveyUseCurrentLocation.setEnabled(true);
                new MaterialAlertDialogBuilder(SurveyFormActivity.this)
                        .setTitle("Location Detection")
                        .setMessage(errorMessage + "\n\nYou can enter the village location manually without losing any entered animal details.")
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
            binding.tvSurveySourcePill.setText("GPS Detected");
            binding.tvSurveySourcePill.setBackgroundResource(R.drawable.bg_badge_gps);
            binding.tvSurveySourcePill.setTextColor(0xFF2E7D32); // Green
            if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                binding.tvSurveyCoordinatesDisplay.setText(String.format(Locale.US, "Lat: %.4f, Lng: %.4f", currentLatitude, currentLongitude));
            } else {
                binding.tvSurveyCoordinatesDisplay.setText("GPS active");
            }
        } else {
            binding.tvSurveySourcePill.setText("Manual Entry Mode");
            binding.tvSurveySourcePill.setBackgroundResource(R.drawable.bg_badge_manual);
            binding.tvSurveySourcePill.setTextColor(0xFFE65100); // Orange
            if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                binding.tvSurveyCoordinatesDisplay.setText(String.format(Locale.US, "Coords: %.4f, %.4f", currentLatitude, currentLongitude));
            } else {
                binding.tvSurveyCoordinatesDisplay.setText("GPS not required");
            }
        }
    }

    private void addAnimalEntryView(AnimalSurvey prefillSurvey) {
        ItemAnimalSurveyEntryBinding itemBinding = ItemAnimalSurveyEntryBinding.inflate(
                LayoutInflater.from(this),
                binding.llAnimalsContainer,
                false
        );

        AnimalEntryHolder holder = new AnimalEntryHolder();
        holder.itemBinding = itemBinding;
        animalHolders.add(holder);

        // Setup Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Constants.CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        itemBinding.spinnerCategory.setAdapter(catAdapter);

        // Category Selection Listener
        itemBinding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                holder.selectedCategory = Constants.CATEGORIES[position];
                updateAnimalTypeSpinner(holder, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Prefill if editing
        if (prefillSurvey != null) {
            for (int i = 0; i < Constants.CATEGORIES.length; i++) {
                if (Constants.CATEGORIES[i].equalsIgnoreCase(prefillSurvey.getAnimalCategory())) {
                    itemBinding.spinnerCategory.setSelection(i);
                    holder.selectedCategory = Constants.CATEGORIES[i];
                    break;
                }
            }
            updateAnimalTypeSpinner(holder, prefillSurvey.getAnimalType());
            if ("Other".equalsIgnoreCase(prefillSurvey.getAnimalType()) || prefillSurvey.getAnimalType().toLowerCase().contains("other")) {
                itemBinding.tilCustomAnimalType.setVisibility(View.VISIBLE);
                itemBinding.etCustomAnimalType.setText(prefillSurvey.getCustomAnimalType());
            }
            itemBinding.etQuantity.setText(String.valueOf(prefillSurvey.getQuantity()));
            itemBinding.etBreed.setText(prefillSurvey.getBreed());
            itemBinding.etAge.setText(prefillSurvey.getAge());
            itemBinding.etGender.setText(prefillSurvey.getGender());
        } else {
            updateAnimalTypeSpinner(holder, null);
        }

        // Remove Button Click
        itemBinding.btnRemoveAnimal.setOnClickListener(v -> {
            if (animalHolders.size() > 1) {
                binding.llAnimalsContainer.removeView(itemBinding.getRoot());
                animalHolders.remove(holder);
                refreshAnimalHeaders();
            } else {
                Toast.makeText(this, "At least one animal entry is required", Toast.LENGTH_SHORT).show();
            }
        });

        binding.llAnimalsContainer.addView(itemBinding.getRoot());
        refreshAnimalHeaders();
    }

    private void updateAnimalTypeSpinner(AnimalEntryHolder holder, String preselectedType) {
        List<String> types = Constants.getTypesForCategory(holder.selectedCategory);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.itemBinding.spinnerAnimalType.setAdapter(typeAdapter);

        if (preselectedType != null) {
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i).equalsIgnoreCase(preselectedType)) {
                    holder.itemBinding.spinnerAnimalType.setSelection(i);
                    holder.selectedType = types.get(i);
                    break;
                }
            }
        } else if (!types.isEmpty()) {
            holder.selectedType = types.get(0);
        }

        holder.itemBinding.spinnerAnimalType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = types.get(position);
                holder.selectedType = selected;
                if (selected.toLowerCase().contains("other")) {
                    holder.itemBinding.tilCustomAnimalType.setVisibility(View.VISIBLE);
                } else {
                    holder.itemBinding.tilCustomAnimalType.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void refreshAnimalHeaders() {
        for (int i = 0; i < animalHolders.size(); i++) {
            AnimalEntryHolder h = animalHolders.get(i);
            h.itemBinding.tvAnimalIndexHeader.setText("Animal / Pet #" + (i + 1));
            // Show delete button only if there are multiple entries and not in edit mode
            if (animalHolders.size() > 1 && !isEditMode) {
                h.itemBinding.btnRemoveAnimal.setVisibility(View.VISIBLE);
            } else {
                h.itemBinding.btnRemoveAnimal.setVisibility(View.GONE);
            }
        }

        if (!isEditMode) {
            if (animalHolders.size() > 1) {
                binding.btnSaveSurvey.setText("Save " + animalHolders.size() + " Animal Records");
            } else {
                binding.btnSaveSurvey.setText("Save Survey Record");
            }
        }
    }

    private void showDatePicker() {
        hideKeyboard();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 12, 0);
            selectedDateMillis = selected.getTimeInMillis();
            binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDateMillis));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Listen for Villages
        villagesListener = repository.listenToVillages(new FirebaseRepository.DataCallback<List<Village>>() {
            @Override
            public void onSuccess(List<Village> data) {
                binding.progressBar.setVisibility(View.GONE);
                villageList = data != null ? data : new ArrayList<>();
                setupVillageAutoComplete();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        // Listen for Owners
        ownersListener = repository.listenToOwners(null, new FirebaseRepository.DataCallback<List<Owner>>() {
            @Override
            public void onSuccess(List<Owner> data) {
                allOwnersList = data != null ? data : new ArrayList<>();
                setupOwnerAutoComplete();
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void setupVillageAutoComplete() {
        if (!preselectedVillageId.isEmpty() && !isEditMode) {
            for (Village v : villageList) {
                if (v.getId().equals(preselectedVillageId)) {
                    binding.etVillageName.setText(v.getName());
                    binding.etLocality.setText(v.getLocality());
                    binding.etPincode.setText(v.getPincode());
                    binding.etDistrict.setText(v.getDistrict());
                    binding.etState.setText(v.getState());
                    binding.etCountry.setText(v.getCountry());
                    currentLatitude = v.getLatitude();
                    currentLongitude = v.getLongitude();
                    currentLocationSource = v.getLocationSource();
                    updateSourceBadge();
                    break;
                }
            }
        }

        binding.etVillageName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilVillageName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupOwnerAutoComplete() {
        if (!preselectedOwnerId.isEmpty() && !isEditMode) {
            for (Owner o : allOwnersList) {
                if (o.getId().equals(preselectedOwnerId)) {
                    binding.etOwnerName.setText(o.getName());
                    binding.etOwnerContact.setText(o.getContactNumber());
                    break;
                }
            }
        }

        binding.etOwnerName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilOwnerName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void validateAndSave() {
        hideKeyboard();

        String villageName = binding.etVillageName.getText() != null ? binding.etVillageName.getText().toString().trim() : "";
        String locality = binding.etLocality.getText() != null ? binding.etLocality.getText().toString().trim() : "";
        String pincode = binding.etPincode.getText() != null ? binding.etPincode.getText().toString().trim() : "";
        String district = binding.etDistrict.getText() != null ? binding.etDistrict.getText().toString().trim() : "";
        String state = binding.etState.getText() != null ? binding.etState.getText().toString().trim() : "";
        String country = binding.etCountry.getText() != null ? binding.etCountry.getText().toString().trim() : "India";

        String ownerName = binding.etOwnerName.getText() != null ? binding.etOwnerName.getText().toString().trim() : "";
        String ownerContact = binding.etOwnerContact.getText() != null ? binding.etOwnerContact.getText().toString().trim() : "";
        String generalNotes = binding.etNotes.getText() != null ? binding.etNotes.getText().toString().trim() : "";

        if (villageName.isEmpty()) {
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

        if (ownerName.isEmpty()) {
            binding.tilOwnerName.setError("Owner name is required");
            binding.etOwnerName.requestFocus();
            binding.scrollView.smoothScrollTo(0, binding.tilOwnerName.getTop());
            return;
        } else {
            binding.tilOwnerName.setError(null);
        }

        if (!ownerContact.isEmpty() && !com.example.vilage_wise.utils.PhoneUtils.isValidIndianPhoneNumber(ownerContact)) {
            binding.tilOwnerContact.setError("Please enter a valid 10-digit mobile number");
            binding.etOwnerContact.requestFocus();
            binding.scrollView.smoothScrollTo(0, binding.tilOwnerContact.getTop());
            return;
        } else {
            binding.tilOwnerContact.setError(null);
        }

        if (animalHolders.isEmpty()) {
            Toast.makeText(this, "Please add at least one animal entry", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate all animal cards
        List<AnimalSurvey> pendingSurveys = new ArrayList<>();

        for (int i = 0; i < animalHolders.size(); i++) {
            AnimalEntryHolder holder = animalHolders.get(i);
            ItemAnimalSurveyEntryBinding ib = holder.itemBinding;

            String category = holder.selectedCategory;
            String animalType = holder.selectedType;
            if (animalType == null || animalType.isEmpty()) {
                animalType = "Other";
            }

            String customType = "";
            if (animalType.toLowerCase().contains("other")) {
                customType = ib.etCustomAnimalType.getText() != null ? ib.etCustomAnimalType.getText().toString().trim() : "";
                if (customType.isEmpty()) {
                    ib.tilCustomAnimalType.setError("Please specify animal type for Animal #" + (i + 1));
                    ib.etCustomAnimalType.requestFocus();
                    binding.scrollView.smoothScrollTo(0, ib.getRoot().getTop());
                    return;
                } else {
                    ib.tilCustomAnimalType.setError(null);
                }
            }

            String qtyStr = ib.etQuantity.getText() != null ? ib.etQuantity.getText().toString().trim() : "";
            int quantity = 0;
            try {
                quantity = Integer.parseInt(qtyStr);
            } catch (NumberFormatException ignored) {
            }

            if (quantity <= 0) {
                ib.tilQuantity.setError("Please enter count (> 0) for Animal #" + (i + 1));
                ib.etQuantity.requestFocus();
                binding.scrollView.smoothScrollTo(0, ib.getRoot().getTop());
                return;
            } else {
                ib.tilQuantity.setError(null);
            }

            String breed = ib.etBreed.getText() != null ? ib.etBreed.getText().toString().trim() : "";
            String age = ib.etAge.getText() != null ? ib.etAge.getText().toString().trim() : "";
            String gender = ib.etGender.getText() != null ? ib.etGender.getText().toString().trim() : "";

            User cachedOfficer = com.example.vilage_wise.utils.SessionManager.getInstance(this).getCachedUser();
            String surveyorId = cachedOfficer != null ? cachedOfficer.getUid() : "";
            String surveyorName = cachedOfficer != null ? cachedOfficer.getFullName() : "Survey Officer";
            String surveyorEmail = cachedOfficer != null ? cachedOfficer.getEmail() : "";

            AnimalSurvey survey = new AnimalSurvey(
                    "",
                    "",
                    "",
                    "",
                    "",
                    category,
                    animalType,
                    customType,
                    quantity,
                    breed,
                    age,
                    gender,
                    generalNotes,
                    selectedDateMillis,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
            );
            survey.setSurveyorId(surveyorId);
            survey.setSurveyorName(surveyorName);
            survey.setSurveyorEmail(surveyorEmail);

            // Location attributes
            survey.setLocality(locality);
            survey.setPincode(pincode);
            survey.setDistrict(district);
            survey.setState(!state.isEmpty() ? state : "Tamil Nadu");
            survey.setCountry(country);
            survey.setLatitude(currentLatitude);
            survey.setLongitude(currentLongitude);
            survey.setLocationSource(currentLocationSource);

            pendingSurveys.add(survey);
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveSurvey.setEnabled(false);

        // Safety timeout
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (binding.progressBar.getVisibility() == View.VISIBLE) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSaveSurvey.setEnabled(true);
                Toast.makeText(SurveyFormActivity.this, "Save timed out. Please check your internet connection and Firebase rules.", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000);

        // 1. Resolve Village in memory
        Village targetVillage = null;
        for (Village v : villageList) {
            if (v.getName().trim().equalsIgnoreCase(villageName)) {
                targetVillage = v;
                break;
            }
        }
        boolean isNewVillage = false;
        if (targetVillage == null) {
            String vId = repository.getDb().collection(Constants.COLLECTION_VILLAGES).document().getId();
            String vDistrict = !district.isEmpty() ? district : "Main District";
            String vState = !state.isEmpty() ? state : "Tamil Nadu";
            targetVillage = new Village(
                    vId,
                    villageName,
                    locality,
                    pincode,
                    vDistrict,
                    vState,
                    country,
                    "",
                    currentLatitude,
                    currentLongitude,
                    currentLocationSource,
                    System.currentTimeMillis()
            );
            isNewVillage = true;
        }

        // 2. Resolve Owner in memory
        Owner targetOwner = null;
        for (Owner o : allOwnersList) {
            if (o.getName().trim().equalsIgnoreCase(ownerName) && 
               (o.getVillageId().equals(targetVillage.getId()) || villageName.equalsIgnoreCase(o.getVillageName()))) {
                targetOwner = o;
                break;
            }
        }
        boolean isNewOwner = false;
        if (targetOwner == null) {
            String oId = repository.getDb().collection(Constants.COLLECTION_OWNERS).document().getId();
            targetOwner = new Owner(oId, ownerName, ownerContact, targetVillage.getId(), targetVillage.getName(), "", System.currentTimeMillis());
            isNewOwner = true;
        }

        final Village resolvedVillage = targetVillage;
        final Owner resolvedOwner = targetOwner;

        if (isEditMode && existingSurvey != null && !pendingSurveys.isEmpty()) {
            AnimalSurvey first = pendingSurveys.get(0);
            existingSurvey.setVillageId(resolvedVillage.getId());
            existingSurvey.setVillageName(resolvedVillage.getName());
            existingSurvey.setLocality(locality);
            existingSurvey.setPincode(pincode);
            existingSurvey.setDistrict(district);
            existingSurvey.setState(!state.isEmpty() ? state : "Tamil Nadu");
            existingSurvey.setCountry(country);
            existingSurvey.setLatitude(currentLatitude);
            existingSurvey.setLongitude(currentLongitude);
            existingSurvey.setLocationSource(currentLocationSource);

            existingSurvey.setOwnerId(resolvedOwner.getId());
            existingSurvey.setOwnerName(resolvedOwner.getName());
            existingSurvey.setAnimalCategory(first.getAnimalCategory());
            existingSurvey.setAnimalType(first.getAnimalType());
            existingSurvey.setCustomAnimalType(first.getCustomAnimalType());
            existingSurvey.setQuantity(first.getQuantity());
            existingSurvey.setBreed(first.getBreed());
            existingSurvey.setAge(first.getAge());
            existingSurvey.setGender(first.getGender());
            existingSurvey.setNotes(generalNotes);
            existingSurvey.setSurveyDate(selectedDateMillis);

            repository.updateSurvey(existingSurvey, new FirebaseRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    binding.progressBar.setVisibility(View.GONE);
                    showSuccessDialogAndNavigateHome(
                            "Survey Updated! 🎉",
                            "Survey record for " + resolvedOwner.getName() + " in " + resolvedVillage.getName() + " has been updated."
                    );
                }

                @Override
                public void onError(Exception e) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveSurvey.setEnabled(true);
                    Toast.makeText(SurveyFormActivity.this, "Update error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Assign village & owner to all pending survey records
            for (AnimalSurvey s : pendingSurveys) {
                s.setVillageId(resolvedVillage.getId());
                s.setVillageName(resolvedVillage.getName());
                s.setOwnerId(resolvedOwner.getId());
                s.setOwnerName(resolvedOwner.getName());
            }

            // Save Village (if new), Owner (if new), and all Surveys in atomic batch
            repository.saveSurveyComplete(resolvedVillage, isNewVillage, resolvedOwner, isNewOwner, pendingSurveys, new FirebaseRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    binding.progressBar.setVisibility(View.GONE);
                    String message = pendingSurveys.size() == 1
                            ? "Saved survey for " + resolvedOwner.getName() + " in " + resolvedVillage.getName() + "!"
                            : "Successfully saved " + pendingSurveys.size() + " animal records for " + resolvedOwner.getName() + " in " + resolvedVillage.getName() + "!";

                    showSuccessDialogAndNavigateHome(
                            "Survey Saved! 🎉",
                            message + "\n\nVillage statistics on your dashboard have been updated."
                    );
                }

                @Override
                public void onError(Exception e) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveSurvey.setEnabled(true);
                    Toast.makeText(SurveyFormActivity.this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showSuccessDialogAndNavigateHome(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_check)
                .setCancelable(false)
                .setPositiveButton("Go to Home Dashboard", (dialog, which) -> {
                    Intent intent = new Intent(SurveyFormActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
        if (ownersListener != null) ownersListener.remove();
    }
}
