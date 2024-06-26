import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

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
            secretKey = generateKey(password, algorithm.equalsIgnoreCase("des") ? "DES" : "AES", keySize);

            // Derive IV from password
            byte[] iv = deriveIVFromPassword(password, ivSize);

            // Initialize Cipher based on mode
            if (mode.equals("CBC")) {
                ivParameterSpec = new IvParameterSpec(iv);
                cipher = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("CFB")) {
                ivParameterSpec = new IvParameterSpec(iv);
                cipher = Cipher.getInstance(algorithm + "/CFB8/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("OFB")) {
                ivParameterSpec = new IvParameterSpec(iv);
                cipher = Cipher.getInstance(algorithm + "/OFB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

            // Decrypt the message
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return decryptedBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
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
            secretKey = generateKey(password, algorithm.equalsIgnoreCase("des") ? "DES" : "AES", keySize);

            // Derive IV from password
            byte[] iv = deriveIVFromPassword(password, ivSize);

            // Initialize Cipher based on mode
            if (mode.equals("CBC")) {
                ivParameterSpec = new IvParameterSpec(iv);
                cipher = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("CFB")) {
                ivParameterSpec = new IvParameterSpec(iv);
                cipher = Cipher.getInstance(algorithm + "/CFB8/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else if (mode.equals("OFB")) {
                ivParameterSpec = new IvParameterSpec(iv);
                cipher = Cipher.getInstance(algorithm + "/OFB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            } else {
                cipher = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }

            // Encrypt the message
            byte[] encryptedBytes = cipher.doFinal(messageBytes);

            return encryptedBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SecretKeySpec generateKey(String password, String algorithm, int keySize) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(key, keySize / 8), algorithm);
    }

    private byte[] deriveIVFromPassword(String password, int ivSize) {
        // Use SHA-256 to hash the password and derive the IV
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] passwordHash = sha.digest(password.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(passwordHash, ivSize); // Use the first 'ivSize' bytes as IV
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new byte[ivSize]; // Return empty IV in case of exception (not expected)
        }
    }

    public static void main(String[] args) {
        String[] algorithms = {"DES", "AES"};
        String[] modes = {"ECB", "CBC", "CFB", "OFB"};
        String password = "MySecretPassword";
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
