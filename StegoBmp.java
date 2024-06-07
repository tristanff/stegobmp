import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.Arrays;


public class StegoBmp {

        public static void main(String[] args) {
            if (args.length < 8) {
                System.out.println("Usage: java StegoBmp -embed -in <file> -b <bitmapfile> -out <bitmapfile> -steg <LSB1 | LSB4 | LSBI> -pass <password> -a <aes128 | aes192 | aes256 | des> -m <ecb | cfb | ofb | cbc>");
                System.out.println("       java StegoBmp -extract -p <bitmapfile> -out <file> -steg <LSB1 | LSB4 | LSBI> -pass <password> -a <aes128 | aes192 | aes256 | des> -m <ecb | cfb | ofb | cbc>");
                return;
            }

            String inFile = null, bmpFile = null, outFile = null, stegMethod = null, password = null;
            String algorithm = null, mode = null;
            boolean embed = false, extract = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-embed":
                        embed = true;
                        break;
                    case "-extract":
                        extract = true;
                        break;
                    case "-in":
                        inFile = args[++i];
                        break;
                    case "-b":
                    case "-p":
                        bmpFile = args[++i];
                        break;
                    case "-out":
                        outFile = args[++i];
                        break;
                    case "-steg":
                        stegMethod = args[++i];
                        break;
                    case "-pass":
                        password = args[++i];
                        break;
                    case "-a":
                        algorithm = args[++i];
                        break;
                    case "-m":
                        mode = args[++i];
                        break;
                }
            }

            if (bmpFile == null || outFile == null || stegMethod == null || password == null || algorithm == null || mode == null) {
                System.out.println("Missing required parameters.");
                return;
            }

            try {
                Cipher cipher = Cipher.getInstance(algorithm + "/" + mode);

                SecretKey secretKey = new SecretKeySpec(password.getBytes(), algorithm);
                IvParameterSpec ivParameterSpec = new IvParameterSpec(password.getBytes());

                if (embed) {
                    if (inFile == null) {
                        System.out.println("Missing input file for embedding.");
                        return;
                    }

                    byte[] fileBytes = Files.readAllBytes(new File(inFile).toPath());
                    int fileSize = fileBytes.length;
                    String fileExtension = getFileExtension(inFile) + "\0";
                    byte[] extBytes = fileExtension.getBytes();
                    byte[] sizeBytes = intToBytes(fileSize);
                    byte[] dataToHide = new byte[sizeBytes.length + fileBytes.length + extBytes.length];

                    System.arraycopy(sizeBytes, 0, dataToHide, 0, sizeBytes.length);
                    System.arraycopy(fileBytes, 0, dataToHide, sizeBytes.length, fileBytes.length);
                    System.arraycopy(extBytes, 0, dataToHide, sizeBytes.length + fileBytes.length, extBytes.length);

                    // Encrypt the data before embedding
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
                    byte[] encryptedData = cipher.doFinal(dataToHide);

                    BufferedImage bmpImage = ImageIO.read(new File(bmpFile));
                    if (bmpImage == null) {
                        System.err.println("Error: The BMP file could not be read. Please check the file path and format.");
                        return;
                    }

                    switch (stegMethod) {
                        case "LSB1":
                            embedLSB1(bmpImage, encryptedData);
                            break;
                        case "LSB4":
                            embedLSB4(bmpImage, encryptedData);
                            break;
                        case "LSBI":
                            embedLSBI(bmpImage, encryptedData);
                            break;
                        default:
                            System.out.println("Invalid steganography method.");
                            return;
                    }

                    ImageIO.write(bmpImage, "bmp", new File(outFile));
                    System.out.println("File embedded successfully.");

                } else if (extract) {
                    BufferedImage bmpImage = ImageIO.read(new File(bmpFile));
                    if (bmpImage == null) {
                        System.err.println("Error: The BMP file could not be read. Please check the file path and format.");
                        return;
                    }

                    byte[] extractedData;
                    switch (stegMethod) {
                        case "LSB1":
                            extractedData = extractLSB1(bmpImage);
                            break;
                        case "LSB4":
                            extractedData = extractLSB4(bmpImage);
                            break;
                        case "LSBI":
                            extractedData = extractLSBI(bmpImage);
                            break;
                        default:
                            System.out.println("Invalid steganography method.");
                            return;
                    }

                    // Decrypt the extracted data
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
                    byte[] decryptedData = cipher.doFinal(extractedData);

                    int fileSize = bytesToInt(Arrays.copyOfRange(decryptedData, 0, 4));
                    int nullTerminatorIndex = 4 + fileSize;
                    while (decryptedData[nullTerminatorIndex] != 0) nullTerminatorIndex++;
                    byte[] fileData = Arrays.copyOfRange(decryptedData, 4, 4 + fileSize);
                    String fileExtension = new String(Arrays.copyOfRange(decryptedData, 4 + fileSize, nullTerminatorIndex));

                    String outputFilePath = outFile;
                    if (outputFilePath.length() > 255) {
                        System.err.println("Error: The output file path is too long.");
                        return;
                    }

                    Files.write(Paths.get(outputFilePath), fileData);
                    System.out.println("File extracted successfully.");

                } else {
                    System.out.println("Specify either -embed or -extract.");
                }
            } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        }

    private static void embedLSB1(BufferedImage img, byte[] data) {
        int dataIndex = 0, dataBitIndex = 0;
        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int bit = (data[dataIndex] >> dataBitIndex) & 1;
                    rgb = (rgb & ~(1 << i)) | (bit << i);
                    if (++dataBitIndex == 8) {
                        dataBitIndex = 0;
                        if (++dataIndex == data.length) break outer;
                    }
                }
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static void embedLSB4(BufferedImage img, byte[] data) {
        int dataIndex = 0, dataNibbleIndex = 0;
        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int nibble = (data[dataIndex] >> (dataNibbleIndex * 4)) & 0xF;
                    rgb = (rgb & ~(0xF << (i * 4))) | (nibble << (i * 4));
                    if (++dataNibbleIndex == 2) {
                        dataNibbleIndex = 0;
                        if (++dataIndex == data.length) break outer;
                    }
                }
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static void embedLSBI(BufferedImage img, byte[] data) {
        int dataIndex = 0, dataBitIndex = 0;
        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    if (dataIndex >= data.length) break outer; // Stop embedding if all data is embedded
                    int bit = (data[dataIndex] >> dataBitIndex) & 1;
                    rgb = (rgb & ~(1 << i)) | (bit << i);
                    if (++dataBitIndex == 8) {
                        dataBitIndex = 0;
                        if (++dataIndex == data.length) break outer;
                    }
                }
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static byte[] extractLSB1(BufferedImage img) {
        byte[] data = new byte[img.getWidth() * img.getHeight() * 3 / 8];
        int dataIndex = 0, dataBitIndex = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int bit = (rgb >> i) & 1;
                    data[dataIndex] = (byte) ((data[dataIndex] & ~(1 << dataBitIndex)) | (bit << dataBitIndex));
                    if (++dataBitIndex == 8) {
                        dataBitIndex = 0;
                        dataIndex++;
                        if (dataIndex == data.length) return data;
                    }
                }
            }
        }
        return data;
    }

    private static byte[] extractLSB4(BufferedImage img) {
        byte[] data = new byte[img.getWidth() * img.getHeight() * 3 / 2];
        int dataIndex = 0, dataNibbleIndex = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int nibble = (rgb >> (i * 4)) & 0xF;
                    data[dataIndex] = (byte) ((data[dataIndex] & ~(0xF << (dataNibbleIndex * 4))) | (nibble << (dataNibbleIndex * 4)));
                    if (++dataNibbleIndex == 2) {
                        dataNibbleIndex = 0;
                        dataIndex++;
                        if (dataIndex == data.length) return data;
                    }
                }
            }
        }
        return data;
    }

    private static byte[] extractLSBI(BufferedImage img) {
        byte[] data = new byte[img.getWidth() * img.getHeight() * 3 / 8];
        int dataIndex = 0, dataBitIndex = 0;
        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int bit = (rgb >> i) & 1;
                    data[dataIndex] = (byte) ((data[dataIndex] & ~(1 << dataBitIndex)) | (bit << dataBitIndex));
                    if (++dataBitIndex == 8) {
                        dataBitIndex = 0;
                        dataIndex++;
                        if (dataIndex == data.length) break outer;
                    }
                }
            }
        }
        return data;
    }

    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    private static byte[] intToBytes(int value) {
        return new byte[] {
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }
}
