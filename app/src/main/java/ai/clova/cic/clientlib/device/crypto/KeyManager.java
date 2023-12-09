package ai.clova.cic.clientlib.device.crypto;

public final class KeyManager {
    public static final KeyManager INSTANCE = new KeyManager();

    static {
        System.loadLibrary("ecdh-native-lib");
    }

    private KeyManager() {
    }

    public static final native String dummyKey();

    public static final native int ecdhGenPublicKey(byte[] bArr, byte[] bArr2);

    public static final native int ecdhSharedSecret(byte[] bArr, byte[] bArr2);
}