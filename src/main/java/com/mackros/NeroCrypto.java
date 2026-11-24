package com.mackros;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Шифрование токена API в конфиге (AES-GCM, ключ выводится из имени пользователя, домашней папки и ОС этого компьютера).
 * Это защита от случайного чтения файла и копирования конфига на другой компьютер, а не от целенаправленного взлома на этой же машине.
 */
final class NeroCrypto {
    private static SecretKeySpec key() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] k = md.digest((System.getProperty("user.name", "") + "|" + System.getProperty("user.home", "") + "|" + System.getProperty("os.name", "") + "|mackros-nero").getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, "AES");
    }

    static String enc(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(s.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) { return ""; }
    }

    static String dec(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            byte[] in = Base64.getDecoder().decode(s);
            byte[] iv = new byte[12];
            System.arraycopy(in, 0, iv, 0, 12);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(c.doFinal(in, 12, in.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    private NeroCrypto() { }
}
