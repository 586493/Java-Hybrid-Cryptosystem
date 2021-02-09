package hybrid.crypto.view;

import hybrid.crypto.algorithms.AsyncTask;
import hybrid.crypto.databases.Card;
import hybrid.crypto.databases.Database;
import hybrid.crypto.databases.Message;
import hybrid.crypto.databases.Mode;
import hybrid.crypto.keys.PrivateRsaKey;
import hybrid.crypto.keys.PublicRsaKey;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.concurrent.Callable;

import static hybrid.crypto.algorithms.Validation.isEmpty;
import static hybrid.crypto.algorithms.Validation.isFileGood;
import static hybrid.crypto.databases.Database.getDatabaseDir;
import static hybrid.crypto.view.Main.*;

public class DatabaseController {
    @FXML private javafx.scene.text.Text databaseName;
    @FXML private ComboBox<Card> cardsComboBox;
    @FXML private Button addCardFromFile;
    @FXML private Button lockBtn;
    @FXML private Button createMyCardBtn;
    @FXML private Button chooseFileToProcBtn;
    @FXML private TextField pathToFileToProc;
    @FXML private Button startProcBtn;
    @FXML private ComboBox<Mode> modeComboBox;
    @FXML private Button removeCardBtn;
    @FXML private Button genNewAsymKeysBtn;

    @Getter @Setter
    private static Database database;
    @Setter private static byte[] pswd;


    private void overwriteDatabaseFile() {
        final Callable<Void> toDo = () -> {
            database.encryptAndSaveDatabase(new String(pswd));
            return null;
        };
        final Callable<Void> onSucceededToDo = () -> {
            return null;
        };
        final Callable<Void> onFailedToDo = () -> {
            return null;
        };
        final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
        asyncTask.noAlertsOnSucceeded();
        asyncTask.startNewThread();
    };

    private void disableAll(final boolean disable) {
        Platform.runLater(() -> {
            databaseName.setDisable(disable);
            cardsComboBox.setDisable(disable);
            addCardFromFile.setDisable(disable);
            lockBtn.setDisable(disable);
            createMyCardBtn.setDisable(disable);
            chooseFileToProcBtn.setDisable(disable);
            pathToFileToProc.setDisable(disable);
            startProcBtn.setDisable(disable);
            modeComboBox.setDisable(disable);
            removeCardBtn.setDisable(disable);
            genNewAsymKeysBtn.setDisable(disable);
        });
    }

    private void updateCardsComboBox() {
        Platform.runLater(() -> {
            cardsComboBox.getItems().clear();
            cardsComboBox.getItems().addAll(database.getCardList());
        });
    }

    @FXML
    private void removeCardBtnClicked(ActionEvent event) {
        final Card card = cardsComboBox.getSelectionModel().getSelectedItem();
        if(card == null) return;
        if(card.getPersonId().equals(database.getOwnerId())) {
            showDialog(
                    "Removing the owner's card forbidden!",
                    Alert.AlertType.ERROR);
            return;
        }
        final Callable<Void> toDo = () -> {
            database.removeCard(card);
            updateCardsComboBox();
            overwriteDatabaseFile();
            return null;
        };
        final Callable<Void> onSucceededToDo = () -> {
            disableAll(false);
            return null;
        };
        final Callable<Void> onFailedToDo = () -> {
            disableAll(false);
            return null;
        };
        disableAll(true);
        final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
        asyncTask.startNewThread();
    }

    @FXML
    private void addCardFromFileClicked(ActionEvent event) {
        final File file = chooseFile();
        if(!isFileGood(file)) return;
        final Callable<Void> toDo = () -> {
            final Card card = Card.fromJsonFile(file);
            database.addCard(card);
            updateCardsComboBox();
            overwriteDatabaseFile();
            return null;
        };
        final Callable<Void> onSucceededToDo = () -> {
            disableAll(false);
            return null;
        };
        final Callable<Void> onFailedToDo = () -> {
            disableAll(false);
            return null;
        };
        disableAll(true);
        final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
        asyncTask.startNewThread();
    }

    @FXML
    private void saveCardToFileBtnClicked(ActionEvent event) {
        final Card card = cardsComboBox.getSelectionModel().getSelectedItem();
        if(card == null) return;
        final Callable<Void> toDo = () -> {
            card.toJsonFile();
            return null;
        };
        final Callable<Void> onSucceededToDo = () -> {
            disableAll(false);
            return null;
        };
        final Callable<Void> onFailedToDo = () -> {
            disableAll(false);
            return null;
        };
        disableAll(true);
        final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
        asyncTask.startNewThread();
    }

    @FXML
    private void chooseFileToProcBtnClicked(ActionEvent event) {
        final File file = chooseFile();
        if(!isFileGood(file)) {
            pathToFileToProc.setText("");
        }
        else {
            pathToFileToProc.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void lockBtnClicked(ActionEvent event) {
        secureRandom.nextBytes(pswd);
        setDatabase(null);
        final Stage stage = (Stage) modeComboBox.getScene().getWindow();
        Platform.runLater(() -> {
            stage.close();
            startWhenDatabaseJustClosed();
        });
    }

    @FXML
    private void genNewAsymKeysBtnClicked(ActionEvent event) {
        final Callable<Void> toDo = () -> {
            database.genNewKeyPairAndReplaceOldKeys();
            updateCardsComboBox();
            overwriteDatabaseFile();
            return null;
        };
        final Callable<Void> onSucceededToDo = () -> {
            disableAll(false);
            return null;
        };
        final Callable<Void> onFailedToDo = () -> {
            disableAll(false);
            return null;
        };
        disableAll(true);
        final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
        asyncTask.startNewThread();
    }

    @FXML
    private void startProcBtnClicked(ActionEvent event) {
        final String filePath = pathToFileToProc.getText();
        if (isEmpty(filePath)) return;
        final File file = new File(filePath);
        if (!isFileGood(file)) {
            showDialog(
                    String.format("Selected file '%s' does not exist!",
                            file.getName()),
                    Alert.AlertType.ERROR
            );
            return;
        }

        final Card card = cardsComboBox.getSelectionModel().getSelectedItem();
        if (card == null) {
            showDialog(
                    "No card selected!",
                    Alert.AlertType.WARNING);
            return;
        }

        final PublicRsaKey pubKey = card.getPersonPubKey();
        final PrivateRsaKey privKey = (card.getPersonId().equals(database.getOwnerId()))
                ? (database.getOwnerPrivKey())
                : (null);

        final Mode mode = modeComboBox.getSelectionModel().getSelectedItem();
        final boolean decOpt = (mode == Mode.DECRYPTION);
        final boolean encOpt = !decOpt;

        if(decOpt && privKey == null) {
            showDialog(
                    "The selected card does not " +
                            "contain a private key!",
                    Alert.AlertType.ERROR
            );
            return;
        }

        /* * * * * * * * * * * * * * * * * * * * * * * * * * */

        final Callable<Void> toDo = () -> {
            if(encOpt) {
                final Message msg = new Message(card, database.getOwnerId(), file);
                msg.encryptAndSaveToJson();
            } else {
                Message.recreateFileFromJson(card, database.getOwnerPrivKey(), file);
            }
            return null;
        };
        final Callable<Void> onSucceededToDo = () -> {
            disableAll(false);
            return null;
        };
        final Callable<Void> onFailedToDo = () -> {
            disableAll(false);
            return null;
        };
        disableAll(true);
        final AsyncTask asyncTask = new AsyncTask(toDo, onSucceededToDo, onFailedToDo);
        asyncTask.startNewThread();
    }

    @FXML
    private void encDecToggleClicked(ActionEvent event) {

    }

    @FXML
    private void modeComboBoxClicked(ActionEvent event) {
        mode();
    }

    private void mode() {
        Mode mode = modeComboBox.getSelectionModel().getSelectedItem();
        if(mode == null) {
            modeComboBox.getSelectionModel().select(0);
            mode = modeComboBox.getSelectionModel().getSelectedItem();
        }
        if(mode == Mode.DECRYPTION) {
            Card toSelect = null;
            for(final Card c : cardsComboBox.getItems()) {
                if(c.getPersonId().equals(database.getOwnerId())){
                    toSelect = c;
                    break;
                }
            }
            if(toSelect != null) {
                cardsComboBox.getSelectionModel().select(toSelect);
                cardsComboBox.setDisable(true);
            }
        }
        else {
            cardsComboBox.setDisable(false);
        }
    }

    public static File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(getDatabaseDir(database.getDatabaseName()));
        final File file = fileChooser.showOpenDialog(null);
        if (file != null && file.exists() && file.isFile()) {
            return file;
        } else {
            return null;
        }
    }

    @FXML
    private void initialize() {
        modeComboBox.getItems().addAll(Mode.values());
        mode();
        databaseName.setText(database.getDatabaseName());
        updateCardsComboBox();
    }
}
