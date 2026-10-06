package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.databinding.ActivityLoginBinding;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.Constants;
import com.example.vilage_wise.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = FirebaseRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        // Pre-seed admin credentials in background if not created yet
        repository.ensureAdminAccount("admin@villagewise.gov", "admin123", null);

        setupListeners();
        setupKeyboardScrollBehavior();
    }

    private void setupKeyboardScrollBehavior() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets imeInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
            androidx.core.graphics.Insets navInsets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int bottomPadding = Math.max(imeInsets.bottom, navInsets.bottom);

            binding.scrollView.setPadding(0, 0, 0, bottomPadding + 32);
            return windowInsets;
        });

        View.OnFocusChangeListener focusListener = (view, hasFocus) -> {
            if (hasFocus) {
                binding.scrollView.postDelayed(() -> {
                    binding.scrollView.smoothScrollBy(0, 160);
                }, 250);
            }
        };

        binding.etEmail.setOnFocusChangeListener(focusListener);
        binding.etPassword.setOnFocusChangeListener(focusListener);
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnAdminQuick.setOnClickListener(v -> {
            binding.etEmail.setText("admin@villagewise.gov");
            binding.etPassword.setText("admin123");
            attemptLogin();
        });
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email address");
            binding.etEmail.requestFocus();
            return;
        } else {
            binding.tilEmail.setError(null);
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError("Please enter your password");
            binding.etPassword.requestFocus();
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        setLoading(true);

        repository.loginUser(email, password, new FirebaseRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                handleUserLoginSuccess(user);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                String msg = e.getMessage() != null ? e.getMessage() : "Invalid credentials";
                if (msg.toLowerCase().contains("invalid") || msg.toLowerCase().contains("user-not-found") || msg.toLowerCase().contains("wrong-password")) {
                    Toast.makeText(LoginActivity.this, "Invalid email or password. Please check and try again.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Login error: " + msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleUserLoginSuccess(User user) {
        if (user == null) {
            Toast.makeText(this, "Could not retrieve user details.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if Admin
        if (Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            sessionManager.saveUserSession(user);
            Toast.makeText(this, "Welcome, Administrator!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Officer Role - Check Approval Status
        if (Constants.STATUS_APPROVED.equalsIgnoreCase(user.getStatus())) {
            sessionManager.saveUserSession(user);
            Toast.makeText(this, "Welcome back, Officer " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (Constants.STATUS_REJECTED.equalsIgnoreCase(user.getStatus())) {
            sessionManager.logout();
            new AlertDialog.Builder(this)
                    .setTitle("Account Not Approved")
                    .setMessage("Your officer registration was rejected by the Administrator. If this is a mistake, please reach out to the Admin team or apply again.")
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            // STATUS_PENDING
            sessionManager.logout();
            new AlertDialog.Builder(this)
                    .setTitle("Account Pending Approval")
                    .setMessage("Hello " + user.getFullName() + ",\n\nYour officer registration has been submitted and is waiting for Administrator approval.\n\nOnce an Admin approves your account in the Admin Control Center, you will be able to log in and record animal surveys.")
                    .setPositiveButton("Understood", null)
                    .show();
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnRegister.setEnabled(!loading);
        binding.btnAdminQuick.setEnabled(!loading);
    }
}
