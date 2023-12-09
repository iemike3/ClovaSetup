package ai.clova.cic.clientlib.device.crypto;

import android.util.Log;

import com.iemike3.clovadeskapp.Utils;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AESCipher {
    public static final AESCipher INSTANCE = new AESCipher();
    private static final String PKCS5_PADDING = "AES/CBC/PKCS5Padding";

    static {
        System.loadLibrary("ecdh-native-lib");
    }

    private AESCipher() {
    }

    public static final native String aes256Iv();

    public static final native String aes256Secret();

    public final String decodeAES(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //t.g(str, "str");
        String aes256Secret = aes256Secret();
        Charset charset = StandardCharsets.UTF_8;
        byte[] bytes = aes256Secret.getBytes(charset);
        //t.f(bytes, "this as java.lang.String).getBytes(charset)");
        byte[] bytes2 = aes256Iv().getBytes(charset);
        //t.f(bytes2, "this as java.lang.String).getBytes(charset)");
        byte[] bytes3 = str.getBytes(charset);
        //t.f(bytes3, "this as java.lang.String).getBytes(charset)");
        //byte[] p10 = a.p(bytes3);
        byte[] p10 = Base64.decodeBase64(bytes3);
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, "AES");
        Cipher cipher = Cipher.getInstance(PKCS5_PADDING);
        cipher.init(2, secretKeySpec, new IvParameterSpec(bytes2));
        byte[] doFinal = cipher.doFinal(p10);
        //t.f(doFinal, "c.doFinal(byteStr)");
        Log.i(this.getClass().getSimpleName(), String.valueOf(doFinal));
        return new String(doFinal, charset);
    }

    public final String decodeAES(byte[] secret, byte[] iv, byte[] str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //t.g(secret, "secret");
        //t.g(iv, "iv");
        //t.g(str, "str");
        //byte[] p10 = a.p(str);
        byte[] p10 = Base64.decodeBase64(str);
        Log.i("decodeAES(byte[] secret, byte[] iv, byte[] str)", Utils.INSTANCE.byteArrayToHexForLog(p10));
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret, "AES");
        Cipher cipher = Cipher.getInstance(PKCS5_PADDING);
        cipher.init(2, secretKeySpec, new IvParameterSpec(iv));
        byte[] doFinal = cipher.doFinal(p10);
        //t.f(doFinal, "c.doFinal(byteStr)");
        Log.i(this.getClass().getSimpleName(), String.valueOf(doFinal));
        return new String(doFinal, StandardCharsets.UTF_8);
    }

    public final String encodeAES(String str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //t.g(str, "str");
        String aes256Secret = aes256Secret();
        Charset charset = StandardCharsets.UTF_8;
        byte[] bytes = aes256Secret.getBytes(charset);
        //t.f(bytes, "this as java.lang.String).getBytes(charset)");
        byte[] bytes2 = aes256Iv().getBytes(charset);
        //t.f(bytes2, "this as java.lang.String).getBytes(charset)");
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, "AES");
        Cipher cipher = Cipher.getInstance(PKCS5_PADDING);
        cipher.init(1, secretKeySpec, new IvParameterSpec(bytes2));
        byte[] bytes3 = str.getBytes(charset);
        //t.f(bytes3, "this as java.lang.String).getBytes(charset)");
        //byte[] q10 = a.q(cipher.doFinal(bytes3));
        byte[] q10 = Base64.encodeBase64(cipher.doFinal(bytes3));
        //t.f(q10, "encodeBase64(encrypted)");
        Log.i(this.getClass().getSimpleName(), String.valueOf(q10));
        return new String(q10, charset);
    }

    public final byte[] encodeAES(byte[] secret, byte[] iv, byte[] str) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //t.g(secret, "secret");
        //t.g(iv, "iv");
        //t.g(str, "str");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret, "AES");
        Cipher cipher = Cipher.getInstance(PKCS5_PADDING);
        cipher.init(1, secretKeySpec, new IvParameterSpec(iv));
        //byte[] q10 = a.q(cipher.doFinal(str));
        byte[] q10 = Base64.encodeBase64(cipher.doFinal(str));
        //t.f(q10, "encodeBase64(encrypted)");
        Log.i(this.getClass().getSimpleName(), String.valueOf(q10));
        return q10;
    }
}