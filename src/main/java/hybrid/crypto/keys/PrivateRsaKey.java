package hybrid.crypto.keys;

import lombok.Getter;
import lombok.NonNull;

import javax.crypto.Cipher;
import java.security.PrivateKey;

public class PrivateRsaKey extends AsymmetricKeys {
    @NonNull @Getter
    private final PrivateKey privateKey;

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    PrivateRsaKey(@NonNull Type type, @NonNull String keyLabel, @NonNull PrivateKey privateKey) throws Exception {
        super(keyLabel, type);
        this.privateKey = privateKey;
    }

    @Override
    public byte[] encrypt(byte[] bytes) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws Exception {
        Cipher cipher = Cipher.getInstance(getType().getCipherType());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(bytes);
    }
}
