package hybrid.crypto.keys;

import lombok.Getter;
import lombok.NonNull;

import javax.crypto.Cipher;
import java.security.PublicKey;

public class PublicRsaKey extends AsymmetricKeys {
    @NonNull
    @Getter
    private final PublicKey publicKey;

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    PublicRsaKey(@NonNull Type type, @NonNull String keyLabel, @NonNull PublicKey publicKey) throws Exception {
        super(keyLabel, type);
        this.publicKey = publicKey;
    }

    @Override
    public byte[] encrypt(byte[] bytes) throws Exception {
        Cipher cipher = Cipher.getInstance(getType().getCipherType());
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(bytes);
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws Exception {
        throw new UnsupportedOperationException();
    }
}
