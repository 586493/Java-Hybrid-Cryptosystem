package hybrid.crypto.keys;

import lombok.NonNull;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static hybrid.crypto.view.Main.randAlphabeticStr;
import static hybrid.crypto.view.Main.secureRandom;

public abstract class Keys implements Serializable {
    protected static final long serialVersionUID = 62L;
    private final static int ID_BYTE_ARR_LENGTH = 12;
    public final static String KEY_EXT = ".mykey";
    public final static int MIN_KEY_LABEL_LENGTH = 5;
    public final static int MAX_LABEL_LENGTH = 30;

    protected final byte[] id;
    protected final byte typeId;
    protected final String keyLabel;

    public abstract byte[] encrypt(byte[] bytes) throws Exception;
    public abstract byte[] decrypt(byte[] bytes) throws Exception;

    protected Keys(@NonNull String keyLabel, @NonNull Type type) {
        this.keyLabel = keyLabel;
        this.typeId = type.getTypeId();
        this.id = new byte[ID_BYTE_ARR_LENGTH];
        secureRandom.nextBytes(this.id);
    }

    String newKeyFileNameWithExt() {
        return (keyLabel + "_" + getType().getShortName() + "_"
                + randAlphabeticStr(8) + KEY_EXT);
    }

    String uniqueAbsolutePath(@NonNull Path dir) {
        while (true) {
            final File file = Paths.get(dir.toString(), newKeyFileNameWithExt()).toFile();
            if(!file.exists()) {
                return file.getAbsolutePath();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keys keys = (Keys) o;
        return Arrays.equals(this.id, keys.id);
    }

    public Type getType() {
        return Type.typeIdByteToEnum(this.typeId);
    }

    public String getKeyLabel() {
        return keyLabel;
    }
}
