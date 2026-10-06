package com.example.vilage_wise.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.databinding.ActivityOwnerFormBinding;
import com.example.vilage_wise.models.Owner;
import com.example.vilage_wise.models.Village;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.PhoneUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class OwnerFormActivity extends AppCompatActivity {

    private ActivityOwnerFormBinding binding;
    private FirebaseRepository repository;
    private Owner existingOwner;
    private boolean isEditMode = false;
    private String preselectedVillageId = "";

    private List<Village> villageList = new ArrayList<>();
    private ListenerRegistration villagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        if (getIntent().hasExtra("owner")) {
            existingOwner = (Owner) getIntent().getSerializableExtra("owner");
            if (existingOwner != null) {
                isEditMode = true;
            }
        }

        if (getIntent().hasExtra("preselectedVillageId")) {
            preselectedVillageId = getIntent().getStringExtra("preselectedVillageId");
        }

        setupUI();
        loadVillages();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (isEditMode) {
            binding.toolbar.setTitle("Edit Owner");
            binding.tvFormTitle.setText("Edit Owner Details");
            binding.etOwnerName.setText(existingOwner.getName());
            binding.etContactNumber.setText(existingOwner.getContactNumber());
            binding.etAddressLocality.setText(existingOwner.getAddressLocality());
            binding.btnSaveOwner.setText("Update Owner");
        } else {
            binding.toolbar.setTitle("Register Owner");
            binding.tvFormTitle.setText("Register Animal Owner");
            binding.btnSaveOwner.setText("Save Owner");
        }

        PhoneUtils.attachPhoneWatcher(binding.etContactNumber);

        binding.btnSaveOwner.setOnClickListener(v -> validateAndSave());
    }

    private void loadVillages() {
        binding.progressBar.setVisibility(View.VISIBLE);
        villagesListener = repository.listenToVillages(new FirebaseRepository.DataCallback<List<Village>>() {
            @Override
            public void onSuccess(List<Village> data) {
                binding.progressBar.setVisibility(View.GONE);
                villageList = data != null ? data : new ArrayList<>();
                setupVillageSpinner();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OwnerFormActivity.this, "Error loading villages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupVillageSpinner() {
        if (villageList.isEmpty()) {
            Toast.makeText(this, "Please add a village first before registering an owner", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        int selectedIndex = 0;
        String targetVillageId = isEditMode ? existingOwner.getVillageId() : preselectedVillageId;

        for (int i = 0; i < villageList.size(); i++) {
            Village v = villageList.get(i);
            labels.add(v.getName() + " (" + v.getDistrict() + ")");
            if (v.getId().equals(targetVillageId)) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerVillage.setAdapter(adapter);
        binding.spinnerVillage.setSelection(selectedIndex);
    }

    private void validateAndSave() {
        if (villageList.isEmpty()) {
            Toast.makeText(this, "No village available. Please create a village first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int villagePos = binding.spinnerVillage.getSelectedItemPosition();
        if (villagePos < 0 || villagePos >= villageList.size()) {
            Toast.makeText(this, "Please select a valid village", Toast.LENGTH_SHORT).show();
            return;
        }

        Village selectedVillage = villageList.get(villagePos);
        String name = binding.etOwnerName.getText() != null ? binding.etOwnerName.getText().toString().trim() : "";
        String contact = binding.etContactNumber.getText() != null ? binding.etContactNumber.getText().toString().trim() : "";
        String address = binding.etAddressLocality.getText() != null ? binding.etAddressLocality.getText().toString().trim() : "";

        if (name.isEmpty()) {
            binding.tilOwnerName.setError("Owner name is required");
            binding.etOwnerName.requestFocus();
            return;
        } else {
            binding.tilOwnerName.setError(null);
        }

        if (!contact.isEmpty() && !PhoneUtils.isValidIndianPhoneNumber(contact)) {
            binding.tilContactNumber.setError("Please enter a valid 10-digit mobile number");
            binding.etContactNumber.requestFocus();
            return;
        } else {
            binding.tilContactNumber.setError(null);
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveOwner.setEnabled(false);

        if (isEditMode) {
            existingOwner.setName(name);
            existingOwner.setVillageId(selectedVillage.getId());
            existingOwner.setVillageName(selectedVillage.getName());
            existingOwner.setContactNumber(contact);
            existingOwner.setAddressLocality(address);

            repository.updateOwner(existingOwner, new FirebaseRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(OwnerFormActivity.this, "Owner updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveOwner.setEnabled(true);
                    Toast.makeText(OwnerFormActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Owner newOwner = new Owner(
                    "",
                    name,
                    contact,
                    selectedVillage.getId(),
                    selectedVillage.getName(),
                    address,
                    System.currentTimeMillis()
            );

            repository.addOwner(newOwner, new FirebaseRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(OwnerFormActivity.this, "Owner registered successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveOwner.setEnabled(true);
                    Toast.makeText(OwnerFormActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (villagesListener != null) villagesListener.remove();
    }
}
