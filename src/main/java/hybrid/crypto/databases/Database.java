package hybrid.crypto.databases;

import hybrid.crypto.keys.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SerializationUtils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.KeySpec;
import java.util.LinkedList;
import java.util.List;

import static hybrid.crypto.keys.Type.AES_1;
import static hybrid.crypto.view.Main.*;

public class Database implements Serializable {
    private static final long serialVersionUID = 11L;
    private static final int SALT_SIZE = 16;
    public static final String EXTENSION = ".profile";

    @Setter
    @Getter
    private String databaseName;
    private final Identifier id;
    private final long creationTime;

    @Getter private final Identifier ownerId;
    private final String ownerName;
    private PublicRsaKey ownerPubKey;
    @Getter private PrivateRsaKey ownerPrivKey;

    @Getter private final List<Card> cardList;


    public Database(@NonNull final String databaseName,
                    @NonNull final String ownerName) throws Exception {
        this.creationTime = System.currentTimeMillis();
        this.cardList = new LinkedList<>();
        this.databaseName = databaseName;
        this.ownerName = ownerName;
        this.id = new Identifier();
        this.ownerId = new Identifier();
        final MyKeyPair myKeyPair = this.genKeyPair();
        this.ownerPubKey = (PublicRsaKey) myKeyPair.publicRsaKey;
        this.ownerPrivKey = (PrivateRsaKey) myKeyPair.privateRsaKey;
        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
        final Card ownerCard = new Card(ownerId, ownerName, ownerPubKey);
        this.cardList.add(ownerCard);
    }

    public void genNewKeyPairAndReplaceOldKeys() throws Exception {
        this.ownerPubKey = null;
        this.ownerPrivKey = null;
        final MyKeyPair myKeyPair = this.genKeyPair();
        this.ownerPubKey = (PublicRsaKey) myKeyPair.publicRsaKey;
        this.ownerPrivKey = (PrivateRsaKey) myKeyPair.privateRsaKey;
        this.cardList.removeIf(c -> c.getPersonId().equals(ownerId));
        this.cardList.add(0, new Card(this.ownerId, this.ownerName, this.ownerPubKey));
        Platform.runLater(() ->
                showDialog("New keys have been generated. " +
                                "All your shared cards are useless now.",
                        Alert.AlertType.INFORMATION));
    }

    private MyKeyPair genKeyPair() throws Exception {
        return AsymmetricKeys.generatePair(
                (secureRandom.nextBoolean()) ? (Type.RSA_1) : (Type.RSA_2),
                byteArrToHexStr(new Identifier().getIdBytes()));
    }

    private Object[] pswdToKeyAndSalt(@NonNull final String password) throws Exception {
        final byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);
        return new Object[]{pswdToKey(password, salt), salt};
    }

    private static AesKey pswdToKey(@NonNull final String password, @NonNull final byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        /* PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength) */
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, AES_1.getKeySize());
        SecretKey tmp = factory.generateSecret(spec);
        return new AesKey(new SecretKeySpec(tmp.getEncoded(), "AES"), AES_1, randAlphabeticStr(SALT_SIZE));
    }

    public static File getDatabaseDir(@NonNull final String databaseName) {
        return Paths.get(USER_DATA_DIR.toFile().getAbsolutePath(), databaseName).toFile();
    }

    public static File getDatabaseFile(@NonNull final String databaseName) {
        final File dir = getDatabaseDir(databaseName);
        if(!dir.exists()) getDatabaseDir(databaseName).mkdir();
        return Paths.get(dir.getAbsolutePath(), (databaseName + EXTENSION)).toFile();
    }

    public void encryptAndSaveDatabase(@NonNull final String password) throws Exception {
        final Object[] keyAndSalt = pswdToKeyAndSalt(password);
        final AesKey aesKey = (AesKey) keyAndSalt[0];
        final byte[] salt = (byte[]) keyAndSalt[1];
        final byte[] enc = aesKey.encrypt(SerializationUtils.serialize(this));
        final byte[] encAndSalt = new byte[enc.length + salt.length];
        System.arraycopy(salt, 0, encAndSalt, 0, salt.length);
        System.arraycopy(enc, 0, encAndSalt, salt.length + 0, enc.length);
        FileOutputStream fos = new FileOutputStream(getDatabaseFile(this.databaseName));
        fos.write(encAndSalt);
        fos.flush();
        fos.close();
    }

    public static Database decryptDatabaseAndLoad(@NonNull final File file,
                                                  @NonNull final String password) throws Exception {
        final byte[] salt = new byte[SALT_SIZE];
        final byte[] encAndSalt = Files.readAllBytes(file.toPath());
        final byte[] enc = new byte[encAndSalt.length - salt.length];
        System.arraycopy(encAndSalt, 0, salt, 0, salt.length);
        System.arraycopy(encAndSalt, salt.length + 0, enc, 0, enc.length);
        final AesKey aesKey = pswdToKey(password, salt);
        final byte[] dec = aesKey.decrypt(enc);
        final Database database = SerializationUtils.deserialize(dec);
        database.setDatabaseName(FilenameUtils.getBaseName(file.getAbsolutePath()));
        return database;
    }

    public void removeCard(@NonNull final Card card) {
        this.cardList.removeIf(c -> c.getPersonId().equals(card.getPersonId()));
    }

    public void addCard(@NonNull final Card card) {
        boolean found = false;
        Card equalCard = null;
        for (final Card c : cardList) {
            if (c.getPersonId().equals(card.getPersonId())) {
                equalCard = c;
                found = true;
                break;
            }
        }

        boolean finalFound = found;
        Card finalEqualCard = equalCard;
        Platform.runLater(() -> {
            if (finalFound) {
                if (card.getCreationTime() > finalEqualCard.getCreationTime()) {
                    // update card
                    final int index = this.cardList.indexOf(finalEqualCard);
                    this.cardList.set(index, card);
                    showDialog(
                            "An existing card has been updated.",
                            Alert.AlertType.INFORMATION
                    );
                } else {
                    showDialog(
                            "The selected card already exists in the database.",
                            Alert.AlertType.INFORMATION);
                }
            } else {
                // add new card
                this.cardList.add(card);
                showDialog(
                        "The selected card has been added to the database.",
                        Alert.AlertType.INFORMATION
                );
            }
        });
    }
}
