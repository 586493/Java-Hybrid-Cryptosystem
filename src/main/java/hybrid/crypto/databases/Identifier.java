package hybrid.crypto.databases;

import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Arrays;

import static hybrid.crypto.view.Main.secureRandom;

public class Identifier implements Serializable {
    private static final long serialVersionUID = 11L;
    private final static int ID_BYTE_ARR_LENGTH = 16;

    @Getter private final byte[] idBytes = new byte[ID_BYTE_ARR_LENGTH];

    public Identifier() {
        secureRandom.nextBytes(this.idBytes);
    }

    public boolean equals(@NonNull final Identifier identifier) {
        return Arrays.equals(this.idBytes, identifier.idBytes);
    }
}
