package hybrid.crypto.keys;

import lombok.NonNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static hybrid.crypto.view.Main.randAlphabeticStr;

public abstract class AsymmetricKeys extends Keys {
    public AsymmetricKeys(@NonNull String keyLabel, @NonNull Type type) {
        super(keyLabel, type);
    }

    public abstract boolean isPublic();
    public abstract boolean isPrivate();

    @Override
    String newKeyFileNameWithExt() {
        String typeInfo = "";
        if(isPublic()) {
            typeInfo += "public_";
        } else if(isPrivate()) {
            typeInfo += "private_";
        }
        return (keyLabel + "_" +
                getType().getShortName()
                + "_" + typeInfo
                + randAlphabeticStr(9) + KEY_EXT);
    }

    public static MyKeyPair generatePair(@NonNull Type type, @NonNull String keyLabel) throws Exception {
        if(type.isSymmetric()) return null;
        if(type == Type.RSA_1 || type == Type.RSA_2) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(type.getKeySize());
            KeyPair keyPair = generator.generateKeyPair();
            final PublicRsaKey publicRsaKey = new PublicRsaKey(type, keyLabel, keyPair.getPublic());
            final PrivateRsaKey privateRsaKey = new PrivateRsaKey(type, keyLabel, keyPair.getPrivate());
            return new MyKeyPair(publicRsaKey, privateRsaKey);
        }
        return null;
    }
}
