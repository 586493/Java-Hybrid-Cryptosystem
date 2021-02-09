package hybrid.crypto.keys;

import lombok.NonNull;

public class MyKeyPair {
    @NonNull public final AsymmetricKeys publicRsaKey;
    @NonNull public final AsymmetricKeys privateRsaKey;

    public MyKeyPair(@NonNull AsymmetricKeys publicRsaKey,
                     @NonNull AsymmetricKeys privateRsaKey) {
        this.publicRsaKey = publicRsaKey;
        this.privateRsaKey = privateRsaKey;
    }
}
