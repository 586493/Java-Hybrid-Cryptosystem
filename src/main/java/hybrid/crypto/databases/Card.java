package hybrid.crypto.databases;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hybrid.crypto.algorithms.Base64Conversion;
import hybrid.crypto.keys.PublicRsaKey;
import hybrid.crypto.view.DatabaseController;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import static hybrid.crypto.algorithms.FileProcessing.getSaveAsOutputFile;
import static hybrid.crypto.databases.Database.getDatabaseDir;
import static hybrid.crypto.view.Main.byteArrToHexStr;

public class Card implements Serializable {
    private static final long serialVersionUID = 11L;

    private static final String TIME_KEY = "time";
    private static final String ID_KEY = "id";
    private static final String NAME_KEY = "name";
    private static final String PUB_KEY_KEY = "pub";

    @Getter private final long creationTime;
    @Getter private final String personName;
    @Getter private final Identifier personId;
    @Getter private final PublicRsaKey personPubKey;

    /**
     * fromJsonFile()
     */
    public Card(long creationTime,
                @NonNull String personName,
                @NonNull Identifier personId,
                @NonNull PublicRsaKey personPubKey) {

        this.creationTime = creationTime;
        this.personName = personName;
        this.personId = personId;
        this.personPubKey = personPubKey;
    }

    public Card(@NonNull Identifier personId, @NonNull String personName,
                @NonNull PublicRsaKey personPubKey) {
        this.creationTime = System.currentTimeMillis();
        this.personId = personId;
        this.personName = personName;
        this.personPubKey = personPubKey;
    }

    public void toJsonFile() throws Exception {
        final File databaseDir = getDatabaseDir(DatabaseController.getDatabase().getDatabaseName());
        final File output = Paths.get(databaseDir.getAbsolutePath(),
                byteArrToHexStr(personId.getIdBytes()) + ".json").toFile();

        final File usersChoice = getSaveAsOutputFile(databaseDir, output,
                "JSON (*.json)", "*.json");

        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator(
                (usersChoice != null) ? (usersChoice) : (output), JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        generator.writeStartObject();
        generator.writeStringField(NAME_KEY, personName);
        generator.writeNumberField(TIME_KEY, creationTime);
        generator.writeStringField(ID_KEY, Base64Conversion.objToBase64Str(personId));
        generator.writeStringField(PUB_KEY_KEY, Base64Conversion.objToBase64Str(personPubKey));
        generator.writeEndObject();
        generator.close();
    }

    public static Card fromJsonFile(@NonNull final File file) throws Exception {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        JsonNode rootNode = mapper.readTree(file);

        long creationTime = -1;
        String personName = "?";
        Identifier personId = null;
        PublicRsaKey personPubKey = null;

        Iterator<Map.Entry<String,JsonNode>> fieldsIterator = rootNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String,JsonNode> field = fieldsIterator.next();
            final String str = field.getValue().toString().replaceAll("\"","");
            switch (field.getKey()) {
                case TIME_KEY:
                    creationTime = Long.parseLong(str);
                    break;
                case ID_KEY:
                    personId = (Identifier) Base64Conversion.base64StrToObj(str);
                    break;
                case NAME_KEY:
                    personName = str;
                    break;
                case PUB_KEY_KEY:
                    personPubKey = (PublicRsaKey) Base64Conversion.base64StrToObj(str);
                    break;
            }
        }
        return new Card(creationTime,personName,personId,personPubKey);
    }

    @Override
    public String toString() {
        final Identifier identifier = DatabaseController.getDatabase().getOwnerId();
        if(identifier.equals(this.personId)) {
            return String.format("%s - profile owner (%s)", personName,
                    byteArrToHexStr(personId.getIdBytes()));
        } else {
            return String.format("%s (%s)", personName,
                    byteArrToHexStr(personId.getIdBytes()));
        }
    }
}
