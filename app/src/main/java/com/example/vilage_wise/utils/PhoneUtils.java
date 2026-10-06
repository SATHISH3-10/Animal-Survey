package com.example.vilage_wise.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class PhoneUtils {

    /**
     * Cleans phone number to strictly 10 digits by stripping spaces, dashes, +91, 91 prefix, or leading 0.
     */
    public static String cleanPhoneNumber(String input) {
        if (input == null) return "";
        // Remove all non-digits
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.startsWith("91") && digits.length() >= 12) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() >= 11) {
            digits = digits.substring(1);
        }
        if (digits.length() > 10) {
            digits = digits.substring(0, 10);
        }
        return digits;
    }

    /**
     * Checks whether the given phone number is a valid 10-digit Indian mobile number.
     */
    public static boolean isValidIndianPhoneNumber(String number) {
        if (number == null) return false;
        String clean = cleanPhoneNumber(number);
        return clean.length() == 10 && clean.matches("^[6-9][0-9]{9}$");
    }

    /**
     * Attaches a TextWatcher that automatically strips +91, country codes, spaces,
     * and non-digits whenever the user types or pastes text.
     */
    public static void attachPhoneWatcher(EditText editText) {
        if (editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting || s == null) return;
                String current = s.toString();
                String cleaned = cleanPhoneNumber(current);
                if (!current.equals(cleaned)) {
                    isFormatting = true;
                    editText.setText(cleaned);
                    editText.setSelection(cleaned.length());
                    isFormatting = false;
                }
            }
        });
    }
}
