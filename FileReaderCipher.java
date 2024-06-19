import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FileReaderCipher {

    private final Encryptor encryptor;
    private String password;

    public FileReaderCipher(String algorithm, String mode, String password) {
        this.encryptor = new Encryptor(algorithm, mode);
        this.password = password;
    }



    // Converts integer to byte array (4 bytes)
    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    // Converts byte array (4 bytes) to integer
    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }

    // Concatenates multiple byte arrays into one
    private byte[] concatenateBytes(byte[]... arrays) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            outputStream.write(array);
        }
        return outputStream.toByteArray();
    }

    // Retrieves file extension including '.', terminated by '\0'
    private String getFileExtension(String filePath) {
        String extension = "";
        int i = filePath.lastIndexOf('.');
        if (i >= 0) {
            extension = filePath.substring(i);
        }
        return extension;
    }

    // Encrypts real size, file data, and extension and returns the steganographic message
    public byte[] encryptAndSteganography(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            String extension = getFileExtension(filePath);
            byte[] realSizeBytes = intToBytes(fileData.length);
            byte[] extensionBytes = (extension + '\0').getBytes(StandardCharsets.UTF_8);

            // Concatenate real size, file data, and extension bytes
            byte[] dataToEncrypt = concatenateBytes(realSizeBytes, fileData, extensionBytes);

            // Encrypt data using Encryptor
            byte[] encryptedData = encryptor.encryptMessage(dataToEncrypt, password);

            // Prepare size of encrypted data
            byte[] encryptedSizeBytes = intToBytes(encryptedData.length);

            // Prepare final steganographic message
            byte[] steganographicMessage = concatenateBytes(encryptedSizeBytes, encryptedData);

            return steganographicMessage;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    // Encrypts the real size, file data, and extension, then steganographically hides the encrypted data
    // Only for debugging purpose
    public void encryptAndHide(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            String extension = getFileExtension(filePath);
            byte[] realSizeBytes = intToBytes(fileData.length);
            byte[] extensionBytes = (extension + '\0').getBytes(StandardCharsets.UTF_8);

            // Concatenate real size, file data, and extension bytes
            byte[] dataToEncrypt = concatenateBytes(realSizeBytes, fileData, extensionBytes);

            // Encrypt data using Encryptor
            byte[] encryptedData = encryptor.encryptMessage(dataToEncrypt, password);

            // Prepare size of encrypted data
            byte[] encryptedSizeBytes = intToBytes(encryptedData.length);

            // Prepare final steganographic message
            byte[] steganographicMessage = concatenateBytes(encryptedSizeBytes, encryptedData);

            // Write steganographic message to file
            String outputFileName = filePath + ".enc"; // Appending ".enc" to original file name
            FileOutputStream outputStream = new FileOutputStream(outputFileName);
            outputStream.write(steganographicMessage);
            outputStream.close();

            System.out.println("Encryption and steganography completed. Output file: " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Decrypts and retrieves the real size, file data, and extension from steganographically hidden data
    // Only for debugging purpose
    public void decryptAndExtract(String filePath) {
        try {
            byte[] steganographicData = Files.readAllBytes(Paths.get(filePath));

            // Extract size of encrypted data
            byte[] encryptedSizeBytes = {steganographicData[0], steganographicData[1], steganographicData[2], steganographicData[3]};
            int encryptedSize = bytesToInt(encryptedSizeBytes);

            // Extract encrypted data
            byte[] encryptedData = new byte[encryptedSize];
            System.arraycopy(steganographicData, 4, encryptedData, 0, encryptedSize);

            // Decrypt encrypted data using Encryptor
            byte[] decryptedData = encryptor.decryptMessage(encryptedData, password);

            // Extract real size, file data, and extension
            byte[] realSizeBytes = {decryptedData[0], decryptedData[1], decryptedData[2], decryptedData[3]};
            int realSize = bytesToInt(realSizeBytes);
            byte[] fileData = new byte[realSize];
            System.arraycopy(decryptedData, 4, fileData, 0, realSize);
            String extension = new String(decryptedData, 4 + realSize, decryptedData.length - (4 + realSize), StandardCharsets.UTF_8);

            // Write decrypted file data to output file
            String outputFileName = "decrypted_" + Paths.get(filePath).getFileName().toString().replaceFirst("[.][^.]+$", "");
            FileOutputStream outputStream = new FileOutputStream(outputFileName);
            outputStream.write(fileData);
            outputStream.close();

            System.out.println("Decryption and file extraction completed. Output file: " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String algorithm = "AES";
        String mode = "CBC";
        String password = "MySecretPassword";

        FileReaderCipher fileReaderCipher = new FileReaderCipher(algorithm, mode, password);

        String filePath = "example.txt";
        // Byte Array contening (Tamaño cifrado || encripcion(tamaño real || datos archivo || extensión)
        byte[] steganographicMessage = fileReaderCipher.encryptAndSteganography(filePath); //
        System.out.println("Steganographic message length: " + steganographicMessage.length);

        // Perform decryption and extraction if needed
        // String encryptedFilePath = "example.txt.enc"; // Example encrypted file path
        // fileReaderCipher.decryptAndExtract(encryptedFilePath);
    }
}