package hybrid.crypto.algorithms;

import lombok.NonNull;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;

public class Base64Conversion {
    public static String objToBase64Str(@NonNull final Object object) {
        final byte[] bytes = SerializationUtils.serialize((Serializable) object);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static Object base64StrToObj(@NonNull final String string) throws Exception {
        byte[] decodedBytes = Base64.getDecoder().decode(string);
        return SerializationUtils.deserialize(decodedBytes);
    }
}
