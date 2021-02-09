package hybrid.crypto.algorithms;

import hybrid.crypto.keys.Keys;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import lombok.NonNull;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.FutureTask;

public abstract class FileProcessing {

    protected static void writeBytesToFile(File file, byte[] bytes) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.flush();
        fos.close();
    }

    public static File getSaveAsOutputFile(
            @NonNull final File databaseDir,
            @NonNull final File defaultOutput,
            @NonNull final String typeDescription,
            @NonNull final String... extensions) throws Exception {

        final FutureTask<File> futureTask = new FutureTask<>(() -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    typeDescription, extensions));
            fileChooser.setInitialDirectory(databaseDir);
            fileChooser.setInitialFileName(defaultOutput.getName());
            return fileChooser.showSaveDialog(null);
        });
        Platform.runLater(futureTask);
        return futureTask.get();
    }

    public static String getExtWithDot(@NonNull File file) {
        return "." + FilenameUtils.getExtension(file.getAbsolutePath());
    }

    public static String getNameWithoutExt(@NonNull File file) {
        return FilenameUtils.getBaseName(file.getAbsolutePath());
    }

    public abstract void processFile(@NonNull File in, @NonNull File out, @NonNull Keys keys) throws Exception;

}
