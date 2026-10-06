package com.example.vilage_wise.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationHelper {

    public static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public static class LocationResult {
        public double latitude = 0.0;
        public double longitude = 0.0;
        public String villageOrLocality = "";
        public String locality = "";
        public String pincode = "";
        public String district = "";
        public String state = "";
        public String country = "India";
        public String locationSource = "GPS";
        public boolean geocodingSucceeded = false;
        public String note = "";
    }

    public interface LocationCallback {
        void onSuccess(LocationResult result);
        void onError(String errorMessage);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static boolean hasLocationPermission(Context context) {
        if (context == null) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGpsProviderEnabled(Context context) {
        if (context == null) return false;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    public static void fetchCurrentLocation(Activity activity, LocationCallback callback) {
        if (activity == null) {
            if (callback != null) callback.onError("Activity context is unavailable.");
            return;
        }

        if (!hasLocationPermission(activity)) {
            if (callback != null) callback.onError("Location permission is required for automatic detection.");
            return;
        }

        if (!isGpsProviderEnabled(activity)) {
            if (callback != null) callback.onError("GPS/Location is turned off on your device. Please enable Location in device settings or enter manually.");
            return;
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(activity);
        CancellationTokenSource cts = new CancellationTokenSource();

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(activity, location -> {
                    if (location != null) {
                        reverseGeocodeLocation(activity.getApplicationContext(), location, callback);
                    } else {
                        // Fallback to last known location
                        fusedClient.getLastLocation().addOnSuccessListener(activity, lastLoc -> {
                            if (lastLoc != null) {
                                reverseGeocodeLocation(activity.getApplicationContext(), lastLoc, callback);
                            } else {
                                if (callback != null) {
                                    callback.onError("Could not acquire GPS coordinates. Please ensure you have GPS reception or use manual entry.");
                                }
                            }
                        }).addOnFailureListener(activity, e -> {
                            if (callback != null) {
                                callback.onError("Failed to obtain GPS coordinates: " + e.getMessage());
                            }
                        });
                    }
                })
                .addOnFailureListener(activity, e -> {
                    if (callback != null) {
                        callback.onError("GPS error: " + e.getMessage());
                    }
                });
    }

    private static void reverseGeocodeLocation(Context context, Location location, LocationCallback callback) {
        final double lat = location.getLatitude();
        final double lng = location.getLongitude();

        executor.execute(() -> {
            LocationResult result = new LocationResult();
            result.latitude = lat;
            result.longitude = lng;
            result.locationSource = "GPS";

            if (!Geocoder.isPresent()) {
                result.geocodingSucceeded = false;
                result.note = "Geocoder is not available on this device. GPS coordinates captured.";
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(result);
                });
                return;
            }

            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 3);

                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    result.geocodingSucceeded = true;

                    String rawSubLocality = addr.getSubLocality() != null ? addr.getSubLocality().trim() : "";
                    String rawLocality = addr.getLocality() != null ? addr.getLocality().trim() : "";
                    String rawSubAdmin = addr.getSubAdminArea() != null ? addr.getSubAdminArea().trim() : "";
                    String rawAdmin = addr.getAdminArea() != null ? addr.getAdminArea().trim() : "";
                    String rawCountry = addr.getCountryName() != null ? addr.getCountryName().trim() : "India";
                    String rawPostalCode = addr.getPostalCode() != null ? addr.getPostalCode().trim() : "";
                    String fullAddressLine = addr.getAddressLine(0) != null ? addr.getAddressLine(0).trim() : "";

                    // 1. State
                    result.state = !rawAdmin.isEmpty() ? rawAdmin : "Tamil Nadu";

                    // 2. Country
                    result.country = !rawCountry.isEmpty() ? rawCountry : "India";

                    // 3. PIN Code
                    if (!rawPostalCode.isEmpty()) {
                        result.pincode = rawPostalCode;
                    } else if (!fullAddressLine.isEmpty()) {
                        // Extract 6 digit PIN from address line if available
                        Pattern pinPattern = Pattern.compile("\\b([1-9][0-9]{5})\\b");
                        Matcher matcher = pinPattern.matcher(fullAddressLine);
                        if (matcher.find()) {
                            result.pincode = matcher.group(1);
                        }
                    }

                    // 4. District Resolution (Crucial: never let District equal State)
                    String resolvedDistrict = "";
                    if (!rawSubAdmin.isEmpty() && !rawSubAdmin.equalsIgnoreCase(result.state) && !rawSubAdmin.equalsIgnoreCase(result.country)) {
                        resolvedDistrict = rawSubAdmin;
                    }

                    // If subLocality is present (e.g. "Vannarpettai"), then rawLocality (e.g. "Tirunelveli") is the District / City!
                    if (resolvedDistrict.isEmpty() && !rawSubLocality.isEmpty() && !rawLocality.isEmpty() && !rawLocality.equalsIgnoreCase(result.state)) {
                        resolvedDistrict = rawLocality;
                    }

                    // If still empty, parse address tokens from fullAddressLine
                    if (resolvedDistrict.isEmpty() && !fullAddressLine.isEmpty()) {
                        resolvedDistrict = extractDistrictFromAddressLine(fullAddressLine, result.state, result.country, rawPostalCode);
                    }

                    // If still empty and locality is not state
                    if (resolvedDistrict.isEmpty() && !rawLocality.isEmpty() && !rawLocality.equalsIgnoreCase(result.state)) {
                        resolvedDistrict = rawLocality;
                    }

                    result.district = resolvedDistrict;

                    // 5. Village / Locality Resolution
                    if (!rawSubLocality.isEmpty()) {
                        // Place has a distinct sublocality/village/area (e.g. "Vannarpettai")
                        result.villageOrLocality = rawSubLocality;
                        result.locality = rawSubLocality;
                    } else if (!rawLocality.isEmpty()) {
                        // Place is a primary village or town (e.g. "Alwarkurichi")
                        result.villageOrLocality = rawLocality;
                        result.locality = rawLocality;
                    } else if (addr.getFeatureName() != null && !addr.getFeatureName().trim().isEmpty()) {
                        String feat = addr.getFeatureName().trim();
                        // Ignore if featureName is just a number / plus code
                        if (!feat.matches("^[0-9+]+$")) {
                            result.villageOrLocality = feat;
                            result.locality = feat;
                        }
                    }

                    // If villageOrLocality is still empty, parse first meaningful token from address line
                    if (result.villageOrLocality.isEmpty() && !fullAddressLine.isEmpty()) {
                        String[] parts = fullAddressLine.split(",");
                        if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                            result.villageOrLocality = parts[0].trim();
                            result.locality = parts[0].trim();
                        }
                    }

                    // Fallback cleanup if district ended up same as state (safeguard)
                    if (result.district.equalsIgnoreCase(result.state)) {
                        if (!rawLocality.isEmpty() && !rawLocality.equalsIgnoreCase(result.state)) {
                            result.district = rawLocality;
                        } else {
                            result.district = "";
                        }
                    }

                } else {
                    result.geocodingSucceeded = false;
                    result.note = "Coordinates detected, but no address details found. You can enter the village and district manually.";
                }

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(result);
                });

            } catch (IOException | IllegalArgumentException e) {
                // Network or geocoder timeout: coordinates are still valid
                result.geocodingSucceeded = false;
                result.note = "GPS coordinates captured successfully. Address lookup unavailable offline; please verify and fill details manually.";
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(result);
                });
            }
        });
    }

    /**
     * Parses district from standard formatted address string:
     * e.g. "Vannarpettai, Tirunelveli, Tamil Nadu 627003, India" -> "Tirunelveli"
     */
    private static String extractDistrictFromAddressLine(String addressLine, String state, String country, String pincode) {
        if (addressLine == null || addressLine.trim().isEmpty()) return "";
        String[] parts = addressLine.split(",");
        List<String> cleanParts = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                cleanParts.add(trimmed);
            }
        }

        if (cleanParts.size() >= 3) {
            // Usually the token before State+PIN is the District/City
            for (int i = 0; i < cleanParts.size(); i++) {
                String part = cleanParts.get(i);
                if (part.toLowerCase().contains(state.toLowerCase()) || (pincode != null && !pincode.isEmpty() && part.contains(pincode))) {
                    if (i > 0) {
                        String candidate = cleanParts.get(i - 1);
                        if (!candidate.equalsIgnoreCase(state) && !candidate.equalsIgnoreCase(country)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return "";
    }

    public static boolean isValidIndianPincode(String pincode) {
        if (pincode == null) return false;
        String clean = pincode.trim();
        // 6 digits, first digit 1-9
        return clean.matches("^[1-9][0-9]{5}$");
    }
}
