package cycredit.io;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PasswordStrengthValidator {
    public enum StrengthLevel {
        WEAK, MEDIUM, STRONG, VERY_STRONG
    }

    /**
     * Checks password strength and missing requirements
     */
    public static PasswordResult validatePassword(String password) {
        List<String> missing = new ArrayList<>();

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[@#$%^&+=!].*");
        boolean hasNoSpace = !password.contains(" ");
        boolean minLength = password.length() >= 8;

        if (!minLength) missing.add("At least 8 characters");
        if (!hasUpper) missing.add("An uppercase letter");
        if (!hasLower) missing.add("A lowercase letter");
        if (!hasDigit) missing.add("A number");
        if (!hasSpecial) missing.add("A special character (@, #, $, %, etc.)");
        if (!hasNoSpace) missing.add("No spaces allowed");

        StrengthLevel level;
        int count = 0;
        if (hasUpper) count++;
        if (hasLower) count++;
        if (hasDigit) count++;
        if (hasSpecial) count++;
        if (minLength) count++;

        if (count <= 2) level = StrengthLevel.WEAK;
        else if (count == 3) level = StrengthLevel.MEDIUM;
        else if (count == 4) level = StrengthLevel.STRONG;
        else level = StrengthLevel.VERY_STRONG;

        return new PasswordResult(level, missing);
    }

    /**
     * Adds real-time validation feedback to an EditText field and a TextView
     */
    public static void attachValidator(final EditText passwordField, final TextView feedbackView) {
        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                PasswordResult result = validatePassword(s.toString());
                StringBuilder msg = new StringBuilder();

                switch (result.level) {
                    case WEAK:
                        msg.append("❌ Weak password\n");
                        break;
                    case MEDIUM:
                        msg.append("⚠️ Medium strength\n");
                        break;
                    case STRONG:
                        msg.append("✅ Strong password\n");
                        break;
                    case VERY_STRONG:
                        msg.append("💪 Very strong password\n");
                        break;
                }

                if (!result.missingRequirements.isEmpty()) {
                    msg.append("Missing:\n");
                    for (String req : result.missingRequirements) {
                        msg.append("• ").append(req).append("\n");
                    }
                }

                feedbackView.setText(msg.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Helper data structure for results
     */
    public static class PasswordResult {
        public StrengthLevel level;
        public List<String> missingRequirements;

        public PasswordResult(StrengthLevel level, List<String> missingRequirements) {
            this.level = level;
            this.missingRequirements = missingRequirements;
        }
    }
}
