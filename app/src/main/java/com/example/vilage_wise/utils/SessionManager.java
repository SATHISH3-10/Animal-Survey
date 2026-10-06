package com.example.vilage_wise.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.vilage_wise.models.User;
import com.google.firebase.auth.FirebaseAuth;

public class SessionManager {
    private static final String PREF_NAME = "village_wise_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "full_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_STATUS = "status";
    private static final String KEY_BADGE = "officer_badge";
    private static final String KEY_VILLAGE = "assigned_village";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveUserSession(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_UID, user.getUid());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_NAME, user.getFullName());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_STATUS, user.getStatus());
        editor.putString(KEY_BADGE, user.getOfficerBadgeId());
        editor.putString(KEY_VILLAGE, user.getAssignedVillage());
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equalsIgnoreCase(prefs.getString(KEY_ROLE, ""));
    }

    public boolean isApproved() {
        return Constants.STATUS_APPROVED.equalsIgnoreCase(prefs.getString(KEY_STATUS, ""));
    }

    public User getCachedUser() {
        if (!isLoggedIn()) return null;
        User user = new User();
        user.setUid(prefs.getString(KEY_UID, ""));
        user.setEmail(prefs.getString(KEY_EMAIL, ""));
        user.setFullName(prefs.getString(KEY_NAME, ""));
        user.setRole(prefs.getString(KEY_ROLE, Constants.ROLE_OFFICER));
        user.setStatus(prefs.getString(KEY_STATUS, Constants.STATUS_PENDING));
        user.setOfficerBadgeId(prefs.getString(KEY_BADGE, ""));
        user.setAssignedVillage(prefs.getString(KEY_VILLAGE, ""));
        return user;
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "Officer");
    }

    public String getUserRole() {
        return prefs.getString(KEY_ROLE, Constants.ROLE_OFFICER);
    }

    public String getAssignedVillage() {
        return prefs.getString(KEY_VILLAGE, "");
    }

    public String getBadgeId() {
        return prefs.getString(KEY_BADGE, "");
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
