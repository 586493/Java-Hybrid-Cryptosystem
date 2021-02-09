package hybrid.crypto.databases;

import lombok.Getter;

public enum Mode {
    ENCRYPTION("encryption"),
    DECRYPTION("decryption"),
    ;

    @Getter
    private final String name;

    private Mode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
