package hybrid.crypto.algorithms;

import hybrid.crypto.keys.*;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FileProcessingTest {

    @Test
    public void encryptDecryptTest() {
        final SecureRandom secureRandom = new SecureRandom();
        final String label = "1234567890";
        for (Type type : Type.values()) {

            byte[] randBytes;
            if (type.isAsymmetric()) {
                randBytes = new byte[382];
            } else {
                randBytes = new byte[24 * 1024];
            }
            secureRandom.nextBytes(randBytes);

            try {
                Keys keys = null;
                MyKeyPair keyPair = null;

                if (type.isAsymmetric()) {
                    keyPair = AsymmetricKeys.generatePair(type, label);
                } else if (type.isSymmetric()) {
                    keys = SymmetricKeys.generateKey(type, label);
                }
                assertTrue("keys == null && keyPair == null",
                        keys != null || keyPair != null);

                final byte[] enc = (keys != null) ?
                        (keys.encrypt(randBytes)) :
                        (keyPair.publicRsaKey.encrypt(randBytes));
                assertFalse(
                        "encrypted text equals to plaintext [" + type.getName() + "]",
                        Arrays.equals(randBytes, enc)
                );

                final byte[] dec = (keys != null) ?
                        (keys.decrypt(enc)) :
                        (keyPair.privateRsaKey.decrypt(enc)) ;
                assertArrayEquals(
                        "decrypted text NOT equals to plaintext [" + type.getName() + "]",
                        randBytes, dec
                );

            } catch (Exception e) {
                String info = "Should not have thrown any exceptions!\n";
                info += "Algorithm: " + type.getName() + "\n";
                info += "Exception message: " + e.getMessage();
                fail(info);
            }
        }
    }
}