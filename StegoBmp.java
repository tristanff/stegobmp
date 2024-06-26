import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


public class StegoBmp{


    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Usage: java StegoBmp -embed -in <file> -b <bitmapfile> -out <bitmapfile> -steg <LSB1 | LSB4 | LSBI> [-pass <password>] [-a <aes128 | aes192 | aes256 | des>] [-m <ecb | cfb | ofb | cbc>]");
            System.out.println("       java StegoBmp -extract -p <bitmapfile> -out <file> -steg <LSB1 | LSB4 | LSBI> [-pass <password>] [-a <aes128 | aes192 | aes256 | des>] [-m <ecb | cfb | ofb | cbc>]");
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
                    bmpFile = args[++i];
                    break;
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
                default:
                    System.out.println("Unknown parameter: " + args[i]);
                    return;
            }
        }

        if (bmpFile == null || outFile == null || stegMethod == null) {
            System.out.println("Missing required parameters.");
            return;
        }

        try {
            Encryptor cipher = new Encryptor("AES", "DES");

            if (password != null) {
                if (algorithm == null) {
                    algorithm = "AES";
                }
                if (mode == null) {
                    mode = "CBC";
                }
                cipher = new Encryptor(algorithm, mode);
            }

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

                // Encrypt the data before embedding if encryption is enabled
                byte[] dataToEmbed = dataToHide;
                if (password != null) {
                    dataToEmbed = cipher.encryptMessage(dataToHide, password);
                }

                //writeHexToFile(dataToEmbed, "embedded_data_hex.txt");
                BMPReader ImageReader = new BMPReader();
                byte[] imageData = ImageReader.readImage(bmpFile);
                if (imageData == null) {
                    System.err.println("Error: The BMP file could not be read. Please check the file path and format.");
                    return;
                }

                byte[] finishData = null;

                switch (stegMethod) {
                    case "LSB1":
                        finishData = StegoImage.stegoLSB1(imageData, dataToEmbed);
                        break;
                    case "LSB4":
                        finishData = StegoImage.stegoLSB4(imageData, dataToEmbed);
                        break;
                    case "LSBI":
                        finishData = StegoImage.stegoLSBI(imageData, dataToEmbed);
                        break;
                    default:
                        System.out.println("Invalid steganography method.");
                        return;
                }

                if (finishData == null) {
                    System.err.println("Error in stego of the data");
                    return;
                }
                Path path = Paths.get(outFile);
                Files.write(path, finishData);
                System.out.println("File embedded successfully.");

            } else if (extract) {
                BMPReader imageReader = new BMPReader();
                byte[] photoToAnalise = imageReader.readImage(bmpFile);
                if (photoToAnalise == null) {
                    System.err.println("Error: The BMP file could not be read. Please check the file path and format.");
                    return;
                }

                byte[] extractedData;
                switch (stegMethod) {
                    case "LSB1":
                        if (password == null) {
                            extractedData = StegoImage.steganalisisLSB1(photoToAnalise, false);    
                        } else{
                            extractedData = StegoImage.steganalisisLSB1(photoToAnalise, true); 
                        }
                        break;
                    case "LSB4":
                        if (password == null) {
                            extractedData = StegoImage.steganalisisLSB4(photoToAnalise, false);    
                        } else{
                            extractedData = StegoImage.steganalisisLSB4(photoToAnalise, true); 
                        }
                        break;
                    case "LSBI":
                        if (password == null) {
                            extractedData = StegoImage.steganalisisLSBI(photoToAnalise, false);    
                        } else{
                            extractedData = StegoImage.steganalisisLSBI(photoToAnalise, true); 
                        }
                        break;
                    default:
                        System.out.println("Invalid steganography method.");
                        return;
                }

                //writeHexToFile(extractedData, "extracted_data_hex.txt");
                // Decrypt the extracted data if encryption is enabled
                byte[] dataToExtract = extractedData;
                byte[] dataDecoded = dataToExtract;
                int fileSize;
                byte[] fileData;
                byte[] extBites;
                if (password != null) {
                    dataDecoded = cipher.decryptMessage(dataToExtract, password);
                    fileSize = bytesToInt(Arrays.copyOfRange(dataDecoded, 0, 4));
                    fileData = Arrays.copyOfRange(dataDecoded, 4, 4 + fileSize);
                    extBites = Arrays.copyOfRange(dataDecoded, 4 + fileSize, dataDecoded.length - 1);
                }else{
                    int len1 = dataDecoded.length;
                    int pospoint = 0;
                    for (int i = 1; len1 - i > 1 && pospoint == 0; i++) {
                        if (dataDecoded[len1-i] == 46) {
                            pospoint = len1 - i;
                        }
                    }
                    fileData = Arrays.copyOfRange(dataDecoded, 0, pospoint);
                    extBites = Arrays.copyOfRange(dataDecoded, pospoint, dataDecoded.length);
                }

               
                
                String extension = new String(extBites);
                String path = outFile.concat(extension);
                Path finalPath = Paths.get(path);
                Files.write(finalPath, fileData);
                
                System.out.println("File extracted successfully.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*private static void embedLSB1(BufferedImage img, byte[] data) {
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
                    int nibble = (data[dataIndex] >> (dataNibbleIndex * 4)) & 0x0F;
                    rgb = (rgb & ~(0x0F << (i * 4))) | (nibble << (i * 4));
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
        // Implementation of LSBI embedding
    }

    private static byte[] extractLSB1(BufferedImage img) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int dataByte = 0, dataBitIndex = 0;

        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int bit = (rgb >> i) & 1;
                    dataByte = (dataByte << 1) | bit;
                    if (++dataBitIndex == 8) {
                        baos.write(dataByte);
                        dataBitIndex = 0;
                        dataByte = 0;
                        if (baos.size() > 4 && baos.size() == bytesToInt(Arrays.copyOfRange(baos.toByteArray(), 0, 4)) + 4) {
                            break outer;
                        }
                    }
                }
            }
        }

        return baos.toByteArray();
    }

    private static byte[] extractLSB4(BufferedImage img) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int dataByte = 0, dataNibbleIndex = 0;

        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                for (int i = 0; i < 3; i++) {
                    int nibble = (rgb >> (i * 4)) & 0x0F;
                    dataByte = (dataByte << 4) | nibble;
                    if (++dataNibbleIndex == 2) {
                        baos.write(dataByte);
                        dataNibbleIndex = 0;
                        dataByte = 0;
                        if (baos.size() > 4 && baos.size() == bytesToInt(Arrays.copyOfRange(baos.toByteArray(), 0, 4)) + 4) {
                            break outer;
                        }
                    }
                }
            }
        }

        return baos.toByteArray();
    }

    private static byte[] extractLSBI(BufferedImage img) {
        // Implementation of LSBI extraction
        return new byte[0];
    }*/

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    /*private static SecretKeySpec generateKey(String password, String algorithm, int keySize) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes());
        return new SecretKeySpec(Arrays.copyOf(key, keySize / 8), algorithm);
    }

    private static IvParameterSpec generateIv(String algorithm) {
        int ivSize = algorithm.equalsIgnoreCase("DES") ? 8 : 16; // 8 bytes for DES, 16 bytes for AES
        byte[] iv = new byte[ivSize];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }*/

    private static String byteToHex(byte b) {
        return String.format("%02X", b);
    }

    private static void writeHexToFile(byte[] data, String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            StringBuilder hexStringBuilder = new StringBuilder();
            for (byte b : data) {
                hexStringBuilder.append(byteToHex(b)).append(" ");
            }
            writer.println(hexStringBuilder.toString().trim());
            System.out.println("Hexadecimal data written to: " + filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
