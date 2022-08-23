package com.waker.util.security;

import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.util.Tools;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

@Slf4j
public class Crypt {

    private static final String SIGNING_ALGORITHM = "HmacSHA256";
    private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final int SALT_BYTES = 24;
    public static final int ITERATION_COUNT = 1000;
    public static final int HASH_BYTES = 24;

    /**
     * Sign a string using SHA256 with a key
     * @param data string to be signed
     * @param key secret key byte array
     * @return signed string
     */
    public static String hmacSHA256(String data, byte[] key) throws TechnicalException {
        try {
            Mac mac = null;
            mac = Mac.getInstance(SIGNING_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, SIGNING_ALGORITHM);
            mac.init(keySpec);
            byte[] signedBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Tools.encode(signedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new TechnicalException(TechnicalErrorCodesAndMessages.ENCRYPTION_ERROR, e.getMessage());
        }
    }

    public static String getSigningAlgorithm() {
        return SIGNING_ALGORITHM;
    }

    /**
     * Creates a hash of a password
     *
     * @param password password string
     * @return hash
     */
    public static String createHash(String password) throws TechnicalException {
        try {
            return createHash(password.toCharArray());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new TechnicalException(TechnicalErrorCodesAndMessages.ENCRYPTION_ERROR, e.getMessage());
        }
    }

    /**
     * Validates string of password
     * @param password password to validate
     * @param goodHash password to validate
     * @return true if valid, false if not
     */
    public static boolean validatePassword(String password, String goodHash) throws TechnicalException {
        try {
            return validatePassword(password.toCharArray(), goodHash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new TechnicalException(TechnicalErrorCodesAndMessages.ENCRYPTION_ERROR, e.getMessage());
        }
    }

    /**
     * Generate a random salt and computes the hash of the character
     * array representation of the password
     *
     * @param password password character array
     * @return hash with format iterationsCount:salt:hash
     */
    private static String createHash(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Generate a random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);

        byte[] hash = computeHash(password, salt, ITERATION_COUNT, HASH_BYTES);
        return ITERATION_COUNT + ":" + Tools.toHex(salt) + ":" + Tools.bytesToHex(hash);
    }

    /**
     * Computes hash of a password.
     *
     * @param password password character array
     * @param salt salt character array
     * @param iterationCount iteration count
     * @param keyLength the to-be-derived key length.
     * @return byte array of the hash
     */
    private static byte[] computeHash(char[] password, byte[] salt, int iterationCount, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterationCount, keyLength * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBE_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }



    /**
     * Validates char representation of password
     * @param password password to validate
     * @param goodHash password to validate
     * @return true if valid, false if not
     */
    private static boolean validatePassword(char[] password, String goodHash) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] parts = goodHash.split(":");
        int iterationCount = Integer.getInteger(parts[0]);
        byte[] salt = Tools.fromHex(parts[1]);
        byte[] hash = Tools.fromHex(parts[2]);

        byte[] newHash = computeHash(password, salt, iterationCount, hash.length);
        return Tools.slowEquals(hash, newHash);
    }

}
