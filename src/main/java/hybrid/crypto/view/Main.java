package hybrid.crypto.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Objects;

import static java.lang.System.exit;

public class Main extends Application {
    private final static String TITLE = " Java-Hybrid-Cryptosystem ";
    private final static String FXML_2_RES = "database_view.fxml";
    private final static String FXML_1_RES = "access_view.fxml";
    private final static String ICON_RES = "/lock.png";
    private final static String CSS_RES = "/stylesheet.css";
    private final static int SCENE_WIDTH = 600;
    private final static int SCENE_HEIGHT = 450;

    public final static SecureRandom secureRandom = new SecureRandom();
    public final static String USER_DATA_DIR_NAME = "Java-Hybrid-Cryptosystem-Data";
    public final static java.nio.file.Path FILESYSTEM_ROOT = File.listRoots()[0].toPath();
    public final static java.nio.file.Path USER_DATA_DIR = Paths.get(FILESYSTEM_ROOT.toString(), USER_DATA_DIR_NAME);

    public static void createDirIfNotExists(final java.nio.file.Path path) {
        path.toFile().mkdir();
    }

    public static String randAlphabeticStr(final int length) {
        final int leftLimit = 97; // 'a'
        final int rightLimit = 122; // 'z'
        return secureRandom.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String byteArrToHexStr(@NonNull final byte[] bytes) {
        String str = "";
        for (byte b : bytes) {
            String tmp = String.format("%2x", b);
            str += tmp.replaceAll(" ", "0");
        }
        return str;
    }

    public static void showDialog(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(null);
        //alert.setContentText(message);
        alert.setHeaderText(null);
        /* * * * * * * * * * * * * * * * * * * * */
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20, 10, 10, 10));
        Text text = new Text(message);
        text.setWrappingWidth(300);
        gridPane.add(text, 0 ,0);
        alert.getDialogPane().setContent(gridPane);
        /* * * * * * * * * * * * * * * * * * * * */
        alert.getDialogPane().setStyle("-fx-font-size: 16;");
        alert.showAndWait();
    }

    public static void errPrintln(Exception e) {
        StackTraceElement l = e.getStackTrace()[0];
        System.err.println("class: " + l.getClassName());
        System.err.println("method: " + l.getMethodName());
        System.err.println("line: " + l.getLineNumber());
        System.err.println("msg: " + e.getMessage());
    }

    public static void startWhenDatabaseJustClosed() {
        Platform.runLater(() -> stageStart(new Stage(), FXML_1_RES));
    }

    public static void startWhenDatabaseJustOpened() {
        Platform.runLater(() -> stageStart(new Stage(), FXML_2_RES));
    }

    private static void stageStart(@NonNull final Stage stage, @NonNull final String fxml) {
        Platform.runLater(() -> {
            Parent root = null;
            try {
                root = FXMLLoader.load(Objects.requireNonNull(
                        Main.class.getClassLoader().getResource(fxml)));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            stage.setTitle(TITLE);
            stage.setResizable(false);
            stage.getIcons().add(new Image(ICON_RES));
            final Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            scene.getStylesheets().add(CSS_RES);
            stage.setOnCloseRequest(e -> {
                System.out.println("exit");
                Platform.exit();
                exit(0);
            });
            stage.show();
        });
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("USER_DATA_DIR: " + USER_DATA_DIR);
        secureRandom.nextBytes(new byte[SCENE_HEIGHT]);
        stageStart(stage, FXML_1_RES);
    }

    public static void main(String[] args) throws Exception {
        createDirIfNotExists(USER_DATA_DIR);
        launch(args);
    }
}
