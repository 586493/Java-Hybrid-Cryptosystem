package hybrid.crypto.algorithms;

import hybrid.crypto.view.Main;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import lombok.NonNull;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class PswdStrength {
    private final static String LEGAL_SPEC_CHARS =
            "!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}";
    private final static double SPEC_CHARS_VALUE = 0.28;
    private final static double NUMBERS_VALUE = 0.26;
    private final static double UPPERCASE_VALUE = 0.23;
    private final static double LOWERCASE_VALUE = 0.23;
    private final static double NORM_MAX = 115;
    private final static double MIN_PWSD_VALUE = 0.705;

    public static boolean isLegalSpecChar(final char ch) {
        return LEGAL_SPEC_CHARS.contains(String.valueOf(ch));
    }

    private static double calcStrength(@NonNull final String str) throws Exception {
        if(str.length() < 1) return 0;

        boolean num = false;
        boolean lower = false;
        boolean upper = false;
        boolean spec = false;

        Set<Character> characters = new HashSet<>();

        for(int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            characters.add(ch);
            if (Character.isDigit(ch)) {
                num = true;
            } else if (Character.isUpperCase(ch)) {
                upper = true;
            } else if (Character.isLowerCase(ch)) {
                lower = true;
            } else if (isLegalSpecChar(ch)) {
                spec = true;
            } else {
                return 0.0;
            }
        }

        double factor = 0.0;
        if(num) factor += NUMBERS_VALUE;
        if(lower) factor += LOWERCASE_VALUE;
        if(upper) factor += UPPERCASE_VALUE;
        if(spec) factor += SPEC_CHARS_VALUE;

        if(factor < 0.0 || factor > 1.0) throw new Exception();

        return factor * log2(Math.pow(characters.size(), str.length()));
    }

    private static double log2(final double x) {
        return (Math.log(x) / Math.log(2));
    }

    /**
     * range [0.0, 1.0]
     */
    public static double calcNormalisedStrength(@NonNull final String str) throws Exception {
        final double e = calcStrength(str);
        final double normalised = (e/NORM_MAX);
        if (normalised >= 1.0) return 1.0;
        else return Math.max(normalised, 0.0);
    }

    public static String getColor(double percent) {
        final double maxH = 0.355;
        final double part = percent * maxH;
        final double H = Math.min(part, maxH); // Hue
        final double S = 0.92; // Saturation
        final double B = 0.92; // Brightness
        final Color color = Color.getHSBColor((float)H, (float)S, (float)B);
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static void updatePswdStrength(@NonNull final ProgressBar bar,
                                          @NonNull final String pswd,
                                          @NonNull final Button createBtn) {
        try {
            final double s = PswdStrength.calcNormalisedStrength(pswd);
            Platform.runLater(() -> {
                bar.setProgress(s);
                bar.setStyle(String.format("-fx-accent: %s;", getColor(s)));
                createBtn.setDisable(s < MIN_PWSD_VALUE);
            });
        } catch (Exception e) {
            Main.errPrintln(e);
        }
    }
}
