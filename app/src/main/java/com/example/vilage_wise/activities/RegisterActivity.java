package com.example.vilage_wise.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.databinding.ActivityRegisterBinding;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.Constants;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnSubmitRegister.setOnClickListener(v -> attemptRegistration());

        setupKeyboardScrollBehavior();
    }

    private void setupKeyboardScrollBehavior() {
        // Dynamically add bottom padding when soft keyboard opens
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets imeInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            androidx.core.graphics.Insets navInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int bottomPadding = Math.max(imeInsets.bottom, navInsets.bottom);

            binding.scrollView.setPadding(0, 0, 0, bottomPadding + 40);
            return windowInsets;
        });

        // Ensure focused fields are scrolled completely into view above keyboard
        View.OnFocusChangeListener focusListener = (view, hasFocus) -> {
            if (hasFocus) {
                binding.scrollView.postDelayed(() -> {
                    int[] location = new int[2];
                    view.getLocationInWindow(location);
                    binding.scrollView.smoothScrollBy(0, 180);
                }, 250);
            }
        };

        binding.etPassword.setOnFocusChangeListener(focusListener);
        binding.etConfirmPassword.setOnFocusChangeListener(focusListener);
        binding.etEmail.setOnFocusChangeListener(focusListener);
        binding.etPhone.setOnFocusChangeListener(focusListener);
        binding.etVillage.setOnFocusChangeListener(focusListener);

        com.example.vilage_wise.utils.PhoneUtils.attachPhoneWatcher(binding.etPhone);
    }

    private void attemptRegistration() {
        String fullName = binding.etFullName.getText() != null ? binding.etFullName.getText().toString().trim() : "";
        String badgeId = binding.etBadgeId.getText() != null ? binding.etBadgeId.getText().toString().trim() : "";
        String village = binding.etVillage.getText() != null ? binding.etVillage.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString().trim() : "";

        // Validations
        if (fullName.isEmpty()) {
            binding.tilFullName.setError("Full name is required");
            binding.etFullName.requestFocus();
            return;
        } else {
            binding.tilFullName.setError(null);
        }

        if (village.isEmpty()) {
            binding.tilVillage.setError("Assigned village/region is required");
            binding.etVillage.requestFocus();
            return;
        } else {
            binding.tilVillage.setError(null);
        }

        if (phone.isEmpty()) {
            binding.tilPhone.setError("Phone number is required");
            binding.etPhone.requestFocus();
            return;
        } else if (!com.example.vilage_wise.utils.PhoneUtils.isValidIndianPhoneNumber(phone)) {
            binding.tilPhone.setError("Please enter a valid 10-digit mobile number");
            binding.etPhone.requestFocus();
            return;
        } else {
            binding.tilPhone.setError(null);
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Valid email address is required");
            binding.etEmail.requestFocus();
            return;
        } else {
            binding.tilEmail.setError(null);
        }

        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        } else {
            binding.tilConfirmPassword.setError(null);
        }

        setLoading(true);

        User officer = new User();
        officer.setFullName(fullName);
        officer.setOfficerBadgeId("PENDING_ASSIGNMENT");
        officer.setAssignedVillage(village);
        officer.setPhone(phone);
        officer.setEmail(email);
        officer.setDesignation("Village Survey Officer");
        officer.setRole(Constants.ROLE_OFFICER);
        officer.setStatus(Constants.STATUS_PENDING);

        repository.registerOfficer(officer, password, new FirebaseRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                showSuccessDialog(fullName, email);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                String msg = e.getMessage() != null ? e.getMessage() : "Registration failed.";
                if (msg.contains("email address is already in use")) {
                    binding.tilEmail.setError("This email is already registered.");
                } else {
                    Toast.makeText(RegisterActivity.this, "Error: " + msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showSuccessDialog(String name, String email) {
        new AlertDialog.Builder(this)
                .setTitle("Application Submitted")
                .setMessage("Officer profile for " + name + " (" + email + ") has been submitted successfully.\n\nYour account is currently in PENDING state awaiting Administrator approval. You will be able to log in and start surveys once approved by the Admin.")
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmitRegister.setEnabled(!loading);
    }
}
