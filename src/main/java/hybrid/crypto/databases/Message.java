package hybrid.crypto.databases;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hybrid.crypto.algorithms.Base64Conversion;
import hybrid.crypto.keys.PrivateRsaKey;
import hybrid.crypto.keys.SymmetricKeys;
import hybrid.crypto.keys.Type;
import hybrid.crypto.view.DatabaseController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.NonNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static hybrid.crypto.algorithms.FileProcessing.getSaveAsOutputFile;
import static hybrid.crypto.algorithms.Validation.isEmpty;
import static hybrid.crypto.databases.Database.getDatabaseDir;
import static hybrid.crypto.keys.SymmetricKeys.generateKey;
import static hybrid.crypto.view.Main.*;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

public class Message implements Serializable {
    private static final long serialVersionUID = 11L;

    private final static String SENDER_ID_KEY = "senderID";
    private final static String RECEIVER_ID_KEY = "receiverID";
    private final static String ENC_FILE_KEY = "data";
    private final static String ENC_SYM_KEY = "key";
    private final static String FILE_NAME_KEY = "fileName";

    private final Card receiversCard;
    private final Identifier senderId;
    private final File file;

    public Message(@NonNull final Card receiversCard,
                   @NonNull final Identifier senderId,
                   @NonNull final File file) throws IOException {

        this.receiversCard = receiversCard;
        this.senderId = senderId;
        this.file = file;
    }

    private static File genOutput(@NonNull final String baseName, @NonNull final String ext) {
        while (true) {
            final File f = Paths.get(
                    getDatabaseDir(DatabaseController.getDatabase().getDatabaseName()).getAbsolutePath(),
                    String.format("%s[%s].%s", baseName, randAlphabeticStr(9), ext)
            ).toFile();
            if (!f.exists()) return f;
        }
    }

    private static class MyGZIPOutputStream extends GZIPOutputStream {
        public MyGZIPOutputStream(OutputStream out) throws IOException {
            super(out);
            def.setLevel(Deflater.BEST_COMPRESSION);
        }
    }

    private String prepareKeyToSend(SymmetricKeys key) throws Exception {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gz = new Message.MyGZIPOutputStream(baos);
                ObjectOutputStream outputStream = new ObjectOutputStream(gz)
        ) {
            outputStream.writeObject(key);
            outputStream.flush();
            gz.close();
            final byte[] bytes = baos.toByteArray();
            final byte[] encBytes = receiversCard.getPersonPubKey().encrypt(bytes);
            return Base64.getEncoder().encodeToString(encBytes);
        }
    }

    private static SymmetricKeys recreateKey(@NonNull final String base64Str,
                                             @NonNull final PrivateRsaKey privateKey) throws Exception {
        final byte[] decByte = privateKey.decrypt(Base64.getDecoder().decode(base64Str));
        byte[] bytesGZIP;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(decByte))) {
            int b;
            while ((b = gis.read()) != -1) {
                baos.write((byte) b);
            }
            bytesGZIP = baos.toByteArray();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytesGZIP))) {
            return (SymmetricKeys) inputStream.readObject();
        }
    }

    public void encryptAndSaveToJson() throws Exception {
        final SymmetricKeys aesKey = generateKey(Type.AES_1, randAlphabeticStr(12));
        if (aesKey == null) throw new NullPointerException("aesKey == null");
        if (file.length() > aesKey.getType().getMaxDataSizeEnc()) {
            final long mb = (aesKey.getType().getMaxDataSizeEnc() / (1024 * 1024)) - 1;
            Platform.runLater(() -> showDialog(
                    String.format("The file '%s' is too large (>%dMB)!", file.getName(), mb),
                    Alert.AlertType.ERROR));
            throw new Exception("file size too large");
        }
        final byte[] inputBytes = Files.readAllBytes(file.toPath());
        final byte[] encBytes = aesKey.encrypt(inputBytes);
        /* * * * * * * * * * * * * * * * * * * * * * * */
        final String ext = "json";
        final File output = genOutput(getBaseName(file.getAbsolutePath()), ext);
        final File databaseDir = getDatabaseDir(DatabaseController.getDatabase().getDatabaseName());
        final File usersChoice = getSaveAsOutputFile(databaseDir, output,
                ("*." + ext), ("*." + ext));
        /* * * * * * * * * * * * * * * * * * * * * * * */
        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator(
                (usersChoice != null) ? (usersChoice) : (output), JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        generator.writeStartObject();
        generator.writeStringField(SENDER_ID_KEY, Base64Conversion.objToBase64Str(senderId));
        generator.writeStringField(RECEIVER_ID_KEY, Base64Conversion.objToBase64Str(receiversCard.getPersonId()));
        generator.writeStringField(FILE_NAME_KEY, file.getName());
        generator.writeStringField(ENC_FILE_KEY, Base64.getEncoder().encodeToString(encBytes));
        generator.writeStringField(ENC_SYM_KEY, prepareKeyToSend(aesKey));
        generator.writeEndObject();
        generator.close();
    }

    private static void writeBytesToFile(@NonNull final File file, @NonNull final byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
        }
    }

    public static void recreateFileFromJson(@NonNull final Card card,
                                            @NonNull final PrivateRsaKey privateRsaKey,
                                            @NonNull final File jsonFile) throws Exception {

        if(!jsonFile.getName().toLowerCase().contains(".json")) {
            Platform.runLater(() -> showDialog("JSON file required!", Alert.AlertType.ERROR));
            throw new Exception("json file required");
        }

        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        JsonNode rootNode = mapper.readTree(jsonFile);

        String receiverStrID = null;
        String fileName = null;
        String base64EncFile = null;
        String base64EncKey = null;

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            final String str = field.getValue().toString().replaceAll("\"", "");
            switch (field.getKey()) {
                case RECEIVER_ID_KEY:
                    receiverStrID = str;
                    break;
                case FILE_NAME_KEY:
                    fileName = str;
                    break;
                case ENC_FILE_KEY:
                    base64EncFile = str;
                    break;
                case ENC_SYM_KEY:
                    base64EncKey = str;
                    break;
            }
        }

        if (isEmpty(receiverStrID) || isEmpty(fileName) || isEmpty(base64EncFile) || isEmpty(base64EncKey)) {
            throw new Exception("json file corrupted");
        }

        @NonNull final Identifier receiverID = (Identifier) Base64Conversion.base64StrToObj(receiverStrID);
        if (!receiverID.equals(card.getPersonId())) {
            Platform.runLater(() -> showDialog(
                    String.format("The file was encrypted with a public key, which " +
                                    "doesn't belong to your key pair (%s ≠ %s).",
                            byteArrToHexStr(receiverID.getIdBytes()),
                            byteArrToHexStr(card.getPersonId().getIdBytes())),
                    Alert.AlertType.ERROR));
            throw new Exception("card ID != receiver ID");
        }

        @NonNull final String ext = getExtension(fileName);
        @NonNull final String nameWithoutExt = getBaseName(fileName);
        @NonNull final byte[] encBytes = Base64.getDecoder().decode(base64EncFile);
        @NonNull final SymmetricKeys key = recreateKey(base64EncKey, privateRsaKey);

        final File output = genOutput(nameWithoutExt, ext);
        final File databaseDir = getDatabaseDir(DatabaseController.getDatabase().getDatabaseName());

        final File usersChoice = getSaveAsOutputFile(databaseDir, output,
                ("*." + ext), ("*." + ext));

        final byte[] decBytes = key.decrypt(encBytes);
        writeBytesToFile((usersChoice != null) ? (usersChoice) : (output), decBytes);
    }
}
