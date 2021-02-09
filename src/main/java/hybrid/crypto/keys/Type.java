package hybrid.crypto.keys;

import lombok.Getter;

/**
 * RSA-4096 (OAEP & SHA-512 & MGF1Padding)
 * RSA-4096 (PKCS1Padding)
 * AES-256 (CBC & PKCS5Padding)
 */
public enum Type {

    /**
     * RSA-4096 (OAEP & SHA-512 & MGF1Padding)
     */
    RSA_1("RSA/ECB/OAEPWithSHA-512AndMGF1Padding",
            "RSA-4096 (OAEP & SHA-512 & MGF1Padding)",
            "RSA-4096",
            4096, false, true,
            (byte) 1,
            380,
            512),

    /**
     * RSA-4096 (PKCS1Padding)
     */
    RSA_2("RSA/ECB/PKCS1Padding",
            "RSA-4096 (PKCS1Padding)",
            "RSA-4096",
            4096, false, true,
            (byte) 2,
            380,
            512),

    /**
     * AES-256 (CBC & PKCS5Padding)
     */
    AES_1("AES/CBC/PKCS5Padding",
            "AES-256 (CBC & PKCS5Padding)",
            "AES-256",
            256, true, false,
            (byte) 3,
            121 * 1024 * 1024,
            171 * 1024 * 1024),

    ;

    @Getter
    private final String cipherType;

    @Getter
    private final String name;

    @Getter
    private final String shortName;

    @Getter
    private final int keySize;

    @Getter
    private final boolean symmetric;

    @Getter
    private final boolean asymmetric;

    @Getter
    private final byte typeId;

    @Getter
    private final long maxDataSizeEnc;

    @Getter
    private final long maxDataSizeDec;

    @Override
    public String toString() {
        return name;
    }

    public static Type typeIdByteToEnum(final byte b) throws IllegalArgumentException {
        for (Type type : Type.values()) {
            if (type.typeId == b) return type;
        }
        throw new IllegalArgumentException();
    }

    private Type(String cipherType, String name, String shortName, int keySize,
                 boolean symmetric, boolean asymmetric, byte typeId, long maxDataSizeEnc, long maxDataSizeDec) {
        this.cipherType = cipherType;
        this.name = name;
        this.shortName = shortName;
        this.keySize = keySize;
        this.symmetric = symmetric;
        this.asymmetric = asymmetric;
        this.typeId = typeId;
        this.maxDataSizeEnc = maxDataSizeEnc;
        this.maxDataSizeDec = maxDataSizeDec;
    }
}
