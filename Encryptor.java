import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Encryptor {

    private final String algorithm;
    private final String mode;
    private final int ivSize; // Size of the initialization vector

    public Encryptor(String algorithm, String mode) {
        this.algorithm = algorithm;
        this.mode = mode.toUpperCase(); // Ensure mode is uppercase for consistency
        this.ivSize = algorithm.equalsIgnoreCase("DES") ? 8 : 16; // DES uses 8 bytes IV, AES uses 16 bytes
    }

    public byte[] decryptMessage(byte[] encryptedBytes, String password) {
        try {
            Cipher cipher = null;
            SecretKey secretKey = null;
            IvParameterSpec ivParameterSpec = null;

            // Determine key size based on algorithm
            int keySize = 128; // Default to AES-128
            if (algorithm.equalsIgnoreCase("aes192")) {
                keySize = 192;
            } else if (algorithm.equalsIgnoreCase("aes256")) {
                keySize = 256;
            } else if (algorithm.equalsIgnoreCase("des")) {
                keySize = 64; // DES key size is 64 bits (8 bytes)
            }

            // Generate secret key from password
            SecretKey derivedKey = generateKey(password, algorithm.equalsIgnoreCase("des") ? "DES" : "AES", keySize);
            secretKey = new SecretKeySpec(Arrays.copyOfRange(derivedKey.getEncoded(), 0, keySize / 8), algorithm);

            // Extract IV from derived key
            ivParameterSpec = extractIV(derivedKey, keySize / 8, ivSize);

            // Initialize Cipher based on mode
            if (mode.equals("CBC")) {
                cipher = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("CFB")) {
                cipher = Cipher.getInstance(algorithm + "/CFB8/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("OFB")) {
                cipher = Cipher.getInstance(algorithm + "/OFB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

            // Decrypt the message
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return decryptedBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] encryptMessage(byte[] messageBytes, String password) {
        try {
            Cipher cipher = null;
            SecretKey secretKey = null;
            IvParameterSpec ivParameterSpec = null;

            // Determine key size based on algorithm
            int keySize = 128; // Default to AES-128
            if (algorithm.equalsIgnoreCase("aes192")) {
                keySize = 192;
            } else if (algorithm.equalsIgnoreCase("aes256")) {
                keySize = 256;
            } else if (algorithm.equalsIgnoreCase("des")) {
                keySize = 64; // DES key size is 64 bits (8 bytes)
            }

            // Generate secret key from password
            SecretKey derivedKey = generateKey(password, algorithm.equalsIgnoreCase("des") ? "DES" : "AES", keySize);
            secretKey = new SecretKeySpec(Arrays.copyOfRange(derivedKey.getEncoded(), 0, keySize / 8), algorithm);

            // Extract IV from derived key
            ivParameterSpec = extractIV(derivedKey, keySize / 8, ivSize);

            // Initialize Cipher based on mode
            if (mode.equals("CBC")) {
                cipher = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("CFB")) {
                cipher = Cipher.getInstance(algorithm + "/CFB8/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("OFB")) {
                cipher = Cipher.getInstance(algorithm + "/OFB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }

            // Encrypt the message
            byte[] encryptedBytes = cipher.doFinal(messageBytes);

            return encryptedBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SecretKey generateKey(String password, String algorithm, int keySize) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Use a fixed salt for PBKDF2
        byte[] salt = {
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };

        // Configure PBKDF2 parameters
        int iterations = 10000; // Number of iterations
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keySize + ivSize * 8);

        // Generate the secret key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] encodedKey = factory.generateSecret(spec).getEncoded();
        System.out.println("Generated Key (Hex): " + bytesToHex(encodedKey));
        // Create and return the SecretKeySpec
        return new SecretKeySpec(encodedKey, algorithm);
    }

    private IvParameterSpec extractIV(SecretKey derivedKey, int keySizeBytes, int blockSizeBytes) {
        byte[] ivBytes = new byte[blockSizeBytes];
        System.arraycopy(derivedKey.getEncoded(), keySizeBytes, ivBytes, 0, blockSizeBytes);
        System.out.println("Extracted IV (Hex): " + bytesToHex(ivBytes));
        return new IvParameterSpec(ivBytes);
    }

    public static void main(String[] args) {
        String[] algorithms = {"DES", "AES"};
        String[] modes = {"ECB", "CBC", "CFB", "OFB"};
        String password = "margarita";
        String messageToEncrypt = "Hello World!";

        for (String algorithm : algorithms) {
            for (String mode : modes) {
                Encryptor encryptor = new Encryptor(algorithm, mode);

                // Convert message to bytes
                byte[] messageBytes = messageToEncrypt.getBytes(StandardCharsets.UTF_8);

                // Encrypt the message
                byte[] encryptedBytes = encryptor.encryptMessage(messageBytes, password);
                System.out.println("Algorithm: " + algorithm + ", Mode: " + mode);
                System.out.println("Encrypted message (hex): " + bytesToHex(encryptedBytes));

                // Decrypt the encrypted message
                byte[] decryptedBytes = encryptor.decryptMessage(encryptedBytes, password);
                if (decryptedBytes != null) {
                    String decryptedMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
                    System.out.println("Decrypted message: " + decryptedMessage);
                } else {
                    System.out.println("Failed to decrypt the message.");
                }
                System.out.println();
            }
        }
    }

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = (byte) HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = (byte) HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();
}
