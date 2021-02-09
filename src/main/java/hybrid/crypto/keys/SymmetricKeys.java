package hybrid.crypto.keys;

import lombok.NonNull;

public abstract class SymmetricKeys extends Keys {
    public SymmetricKeys(@NonNull String keyLabel, @NonNull Type type) {
        super(keyLabel, type);
    }

    public static SymmetricKeys generateKey(@NonNull Type type, @NonNull String keyLabel) throws Exception {
        if(type.isAsymmetric()) return null;
        if(type == Type.AES_1) {
            return new AesKey(type, keyLabel);
        }
        return null;
    }
}
