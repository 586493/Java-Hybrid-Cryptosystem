package hybrid.crypto.keys;

import lombok.NonNull;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static hybrid.crypto.view.Main.secureRandom;

public class AesKey extends SymmetricKeys {
    @NonNull private static final byte[] initVector = new byte[16];
    @NonNull private final byte[] secretKeyBytes;

    /**
     * only for database encryption and decryption
     */
    public AesKey(@NonNull final SecretKey secretKey, @NonNull Type type, @NonNull String keyLabel) {
        super(keyLabel, type);
        this.secretKeyBytes = secretKey.getEncoded();
    }

    AesKey(@NonNull Type type, @NonNull String keyLabel) throws Exception {
        super(keyLabel, type);

        final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(type.getKeySize(), secureRandom);
        final SecretKey secretKey = keyGen.generateKey();
        this.secretKeyBytes = secretKey.getEncoded();
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES");
    }

    @Override
    public byte[] encrypt(byte[] bytes) throws Exception {

        secureRandom.nextBytes(initVector);
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);
        final Cipher cipher = Cipher.getInstance(getType().getCipherType());
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), ivParameterSpec, secureRandom);
        final byte[] encryptedBytes = cipher.doFinal(bytes);

        final byte[] outputBytes = new byte[initVector.length + encryptedBytes.length];
        System.arraycopy(
                initVector, 0,
                outputBytes, 0,
                initVector.length
        );
        System.arraycopy(
                encryptedBytes, 0,
                outputBytes, initVector.length + 0,
                encryptedBytes.length
        );
        return outputBytes;
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws Exception {

        System.arraycopy(
                bytes, 0,
                initVector, 0,
                initVector.length
        );
        final byte[] encryptedBytes = new byte[bytes.length - initVector.length];
        System.arraycopy(
                bytes, 0 + initVector.length,
                encryptedBytes, 0,
                bytes.length - initVector.length
        );

        final IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);
        final Cipher cipher = Cipher.getInstance(getType().getCipherType());
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), ivParameterSpec);
        return cipher.doFinal(encryptedBytes);
    }
}
