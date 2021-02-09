package hybrid.crypto.algorithms;

import javafx.scene.control.Alert;

import java.io.File;
import java.util.regex.Pattern;

import static hybrid.crypto.view.Main.showDialog;

public class Validation {
    public static final int DATABASE_PSWD_MAX_LEN = 52;
    public static final int MIN_NAME_LEN = 6;
    public static final int MAX_NAME_LEN = 26;

    public static boolean isEmpty(final String str) {
        return str == null || str.length() < 1;
    }

    public static boolean isFileGood(final File file) {
        if(file == null) return false;
        else if(!file.exists()) return false;
        else return file.isFile();
    }

    public static boolean isAlphanumeric(final String str) {
        if(isEmpty(str)) return false;
        Pattern p = Pattern.compile("[^a-zA-Z0-9]");
        return !p.matcher(str).find();
    }

    public static boolean isNewNameValid(final String str) {
        if (isEmpty(str) || str.length() < MIN_NAME_LEN) {
            showDialog(
                    String.format(
                            "Min. name length: %d",
                            MIN_NAME_LEN
                    ),
                    Alert.AlertType.WARNING);
            return false;
        } else if (str.length() > MAX_NAME_LEN) {
            showDialog(
                    String.format(
                            "Max. name length: %d",
                            MAX_NAME_LEN
                    ),
                    Alert.AlertType.WARNING);
            return false;
        }

        final boolean alphanumeric = isAlphanumeric(str);

        if (!alphanumeric) {
            showDialog("New names: expected alphanumeric string",
                    Alert.AlertType.WARNING);
        }

        return alphanumeric;
    }

}
