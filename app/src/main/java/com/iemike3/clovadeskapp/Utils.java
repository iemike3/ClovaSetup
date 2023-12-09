package com.iemike3.clovadeskapp;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import kotlin.UByte;

public final class Utils {
    public static final Utils INSTANCE = new Utils();

    private Utils() {
    }

    public static String byteArrayToHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        int length = bArr.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x ", Integer.valueOf(bArr[i] & UByte.MAX_VALUE)));
        }
        return sb.toString();
    }

    public final String byteArrayToHexForLog(byte[] bArr) {
        StringBuilder sb2 = new StringBuilder();
        if (bArr != null) {
            int length = bArr.length;
            int i10 = 0;
            while (i10 < length) {
                byte b10 = bArr[i10];
                i10++;
                //q0 q0Var = q0.a;
                String format = String.format("%02x ", Arrays.copyOf(new Object[]{Integer.valueOf((b10 & 255))}, 1));
                //s.e(format, "format(format, *args)");
                sb2.append(format);
            }
        }
        String sb3 = sb2.toString();
        //s.e(sb3, "sb.toString()");
        return sb3;
    }

    public final byte[] encryptSHA512(byte[] input) throws NoSuchAlgorithmException {
        int a10;
        //Log.i(input, "input");
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(input);
        byte[] byteData = messageDigest.digest();
        StringBuilder sb2 = new StringBuilder();
        //s.e(byteData, "byteData");
        int length = byteData.length;
        int i10 = 0;
        while (i10 < length) {
            byte b10 = byteData[i10];
            i10++;
            a10 = axcaa(16);
            String num = Integer.toString((b10 & 255) + 256, a10);
            //s.e(num, "toString(this, checkRadix(radix))");
            String substring = num.substring(1);
            //s.e(substring, "this as java.lang.String).substring(startIndex)");
            sb2.append(substring);
        }
        Log.i("[Debug] encryptSHA512", sb2.toString());
        return hexStringToByteArray(sb2.toString());
    }

    public static int axcaa(int i10) {
        boolean z10 = false;
        if (2 <= i10 && i10 < 37) {
            z10 = true;
        }
        if (z10) {
            return i10;
        }
        throw new IllegalArgumentException("radix " + i10 + " was not in valid range ");
    }

    public static byte[] hexStringToByteArray(String str) {
        int length = str.length();
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    public final byte[] getAESIV(byte[] input) {
        //t.g(input, "input");
        byte[] bArr = new byte[16];
        System.arraycopy(input, 8, bArr, 0, 16);
        return bArr;
    }

    public final byte[] getAESKey(byte[] input) {
        //t.g(input, "input");
        byte[] bArr = new byte[32];
        System.arraycopy(input, 32, bArr, 0, 32);
        return bArr;
    }

    public final String getUUID(Context context) {
        //t.g(context, "context");
        String androidId = Settings.Secure.getString(context.getContentResolver(), "android_id");
        //t.f(androidId, "androidId");
        Charset UTF_8 = StandardCharsets.UTF_8;
        //t.f(UTF_8, "UTF_8");
        byte[] bytes = androidId.getBytes(UTF_8);
        //t.f(bytes, "this as java.lang.String).getBytes(charset)");
        String uuid = UUID.nameUUIDFromBytes(bytes).toString();
        Log.i(this.getClass().getSimpleName(), uuid + "deviceUuid.toString()");
        return uuid;
    }

}
