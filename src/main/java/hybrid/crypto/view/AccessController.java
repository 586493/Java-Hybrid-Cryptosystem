package hybrid.crypto.view;

import hybrid.crypto.algorithms.AsyncTask;
import hybrid.crypto.algorithms.PswdStrength;
import hybrid.crypto.algorithms.Validation;
import hybrid.crypto.databases.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static hybrid.crypto.algorithms.Validation.*;
import static hybrid.crypto.databases.Database.EXTENSION;
import static hybrid.crypto.databases.Database.getDatabaseFile;
import static hybrid.crypto.view.Main.*;

public class AccessController {
    @FXML private Button createDatabaseBtn;
    @FXML private TextField newDatabasePswd;
    @FXML private TextField newDatabaseUserName;
    @FXML private TextField newDatabaseName;
    @FXML private ProgressBar pswdStrength;
    @FXML private javafx.scene.text.Text newPswdLenText;
    @FXML private javafx.scene.text.Text newUserNameLenText;
    @FXML private javafx.scene.text.Text newDatabaseNameLenText;
    /* ********************************************************** */
    @FXML private ComboBox<ProfileFile> databaseComboBox;
    @FXML private PasswordField openDatabasePswd;
    @FXML private Button openDatabaseBtn;
    @FXML private Button reloadDatabasesBtn;

    private static class ProfileFile implements Comparable<ProfileFile> {
        public final File file;
        private ProfileFile(File file) {
            this.file = file;
        }
        @Override
        public String toString() {
            return file.getAbsolutePath().replaceAll(
                    Pattern.quote(USER_DATA_DIR.toFile().getAbsolutePath()),
                    "");
        }
        @Override
        public int compareTo(ProfileFile o) {
            try {
                if(FileUtils.contentEquals(this.file, o.file)) {
                    return 0;
                }
                else return this.file.getAbsolutePath().
                        compareTo(o.file.getAbsolutePath());
            } catch (IOException e) {
                return -1;
            }
        }
    }

    private Set<ProfileFile> getListOfDatabases() {
        final List<Path> paths = new LinkedList<>();
        try {
            Files.walk(USER_DATA_DIR)
                    .filter(
                            p -> Files.exists(p)
                                    && (!Files.isDirectory(p))
                                    && Files.isRegularFile(p)
                                    && p.toFile().getName().length() > EXTENSION.length()
                                    && p.toFile().getName().endsWith(EXTENSION)
                    )
                    .forEach(paths::add);
        } catch (Exception e) {
            paths.clear();
        }
        final Set<ProfileFile> files = new TreeSet<>();
        for (Path p : paths) files.add(new ProfileFile(p.toFile()));
        openDatabaseBtn.setDisable(files.size() < 1);
        return files;
    }

    @FXML
    private void reloadDatabasesBtnClicked(ActionEvent event) {
        final Set<ProfileFile> set = getListOfDatabases();
        databaseComboBox.getItems().clear();
        databaseComboBox.getItems().addAll(set);
    }

    @FXML
    private void openDatabaseBtnClicked(ActionEvent event) {
        final ProfileFile profileFile = databaseComboBox.getSelectionModel().getSelectedItem();
        if(profileFile == null) return;
        final File file = profileFile.file;
        if (!isFileGood(file)) {
            showDialog(
                    "Selected file does not exist!",
                    Alert.AlertType.ERROR
            );
            return;
        } else {
            final String pswd = openDatabasePswd.getText();
            if (isEmpty(pswd)) {
                showDialog(
                        "Database password required!",
                        Alert.AlertType.WARNING
                );
                return;
            }

            openDatabasePswd.setText("");
            final Stage stage = (Stage) databaseComboBox.getScene().getWindow();

            final Callable<Void> toDo = () -> {
                try {
                    final Database database = Database.decryptDatabaseAndLoad(file, pswd);
                    DatabaseController.setDatabase(database);
                    DatabaseController.setPswd(pswd.getBytes());
                    startWhenDatabaseJustOpened();
                } catch (javax.crypto.BadPaddingException badPaddingException) {
                    Platform.runLater(() ->
                            showDialog(
                                    "Access denied! Wrong password!",
                                    Alert.AlertType.ERROR
                            ));
                    openDatabaseDisable(false);
                    return null;
                }
                Platform.runLater(stage::close);
                return null;
            };
            final Callable<Void> onSucceededToDo = () -> null;
            final Callable<Void> onFailedToDo = () -> {
                DatabaseController.setDatabase(null);
                openDatabaseDisable(false);
                return null;
            };
            openDatabaseDisable(true);
            final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
            asyncTask.noAlertsOnSucceeded();
            asyncTask.startNewThread();
        }
    }

    private void openDatabaseDisable(final boolean disable) {
        Platform.runLater(() -> {
            databaseComboBox.setDisable(disable);
            reloadDatabasesBtn.setDisable(disable);
            openDatabasePswd.setDisable(disable);
            openDatabaseBtn.setDisable(disable);
        });
    }

    private void newDatabaseDisable(final boolean disable) {
        Platform.runLater(() -> {
            newDatabaseUserName.setDisable(disable);
            newDatabaseName.setDisable(disable);
            newDatabasePswd.setDisable(disable);
            createDatabaseBtn.setDisable(disable);
        });
    }

    @FXML
    private void createDatabaseBtnClicked(ActionEvent event) {
        /* Validation */ {
            final boolean userValid = Validation.isNewNameValid(newDatabaseUserName.getText());
            if (!userValid) return;
            final boolean databaseNameValid = Validation.isNewNameValid(newDatabaseName.getText());
            if (!databaseNameValid) return;
        }
        {
            final String userName = newDatabaseUserName.getText();
            final String databaseName = newDatabaseName.getText();
            final String pswd = newDatabasePswd.getText();
            final File f = getDatabaseFile(databaseName);
            if (f.exists()) {
                showDialog(
                        String.format("File '%s' already exists!", f.getName()),
                        Alert.AlertType.ERROR);
                return;
            }

            newDatabasePswd.setText("");
            final Callable<Void> toDo = () -> {
                final Database database = new Database(databaseName, userName);
                database.encryptAndSaveDatabase(pswd);
                return null;
            };
            final Callable<Void> onSucceededToDo = () -> {
                newDatabaseDisable(false);
                return null;
            };
            final Callable<Void> onFailedToDo = () -> {
                newDatabaseDisable(false);
                return null;
            };
            newDatabaseDisable(true);
            final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
            asyncTask.startNewThread();
        }
    }

    @FXML
    private void initialize() {
        createDatabaseBtn.setDisable(true);
        reloadDatabasesBtnClicked(null);

        newDatabaseName.textProperty().addListener((observable, oldValue, newValue) -> {
            final String str = newDatabaseName.getText();
            if(isEmpty(str)) newDatabaseNameLenText.setText(String.valueOf(0));
            else {
                if(str.length() > Validation.MAX_NAME_LEN) Validation.isNewNameValid(str);
                newDatabaseNameLenText.setText(String.valueOf(str.length()));
            }
        });

        newDatabaseUserName.textProperty().addListener((observable, oldValue, newValue) -> {
            final String str = newDatabaseUserName.getText();
            if(isEmpty(str)) newUserNameLenText.setText(String.valueOf(0));
            else {
                if(str.length() > Validation.MAX_NAME_LEN) Validation.isNewNameValid(str);
                newUserNameLenText.setText(String.valueOf(str.length()));
            }
        });

        newDatabasePswd.textProperty().addListener((observable, oldValue, newValue) -> {
            if(newDatabasePswd.getText().length() > DATABASE_PSWD_MAX_LEN) {
                newDatabasePswd.setText(newDatabasePswd.getText().substring(0, DATABASE_PSWD_MAX_LEN));
            }
            newPswdLenText.setText(String.valueOf(newDatabasePswd.getText().length()));
            PswdStrength.updatePswdStrength(pswdStrength, newDatabasePswd.getText(), createDatabaseBtn);
        });
    }
}
