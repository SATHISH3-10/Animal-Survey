package com.example.vilage_wise.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vilage_wise.databinding.ActivitySplashBinding;
import com.example.vilage_wise.repository.FirebaseRepository;
import com.example.vilage_wise.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase repository
        FirebaseRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        // Pre-seed default admin credentials if database is empty
        FirebaseRepository.getInstance().ensureAdminAccount("admin@villagewise.gov", "admin123", null);

        // Delay 1.2 seconds for smooth welcome experience
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent destinationIntent;

            if (sessionManager.isLoggedIn()) {
                if (sessionManager.isAdmin()) {
                    destinationIntent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                } else if (sessionManager.isApproved()) {
                    destinationIntent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // Session not approved or invalid
                    sessionManager.logout();
                    destinationIntent = new Intent(SplashActivity.this, LoginActivity.class);
                }
            } else {
                destinationIntent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(destinationIntent);
            finish();
        }, 1200);
    }
}

