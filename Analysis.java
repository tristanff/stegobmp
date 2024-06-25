import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Analysis {

    public static void main(String[] args) {
        String bmpFile = "data/paris.bmp";

        try {
            BMPReader bmpReader = new BMPReader();
            byte[] bmpData = bmpReader.readImage(bmpFile);

            // Attempt to extract data using the LSBI method
            byte[] extractedLSBI = StegoImage.steganalisisLSBI(bmpData);

            // Find the position of the first ASCII code for '.'
            int dotIndex = -1;
            for (int i = 0; i < extractedLSBI.length - 1; i++) {
                if (extractedLSBI[i] == 46) { // ASCII code for '.'
                    dotIndex = i;
                    break;
                }
            }

            if (dotIndex != -1 && dotIndex < extractedLSBI.length - 1) {
                // Extract the file extension assuming it follows the dot
                byte[] extensionBytes = new byte[extractedLSBI.length - dotIndex - 1];
                System.arraycopy(extractedLSBI, dotIndex + 1, extensionBytes, 0, extensionBytes.length);
                String fileExtension = new String(extensionBytes);

                // Print or use the file extension
                System.out.println("File extension : "+ fileExtension);
            } else {
                System.out.println("File type not found in " + bmpFile);
            }

            // Save the extracted data to a file for examination
            Files.write(Paths.get("test1_" + bmpFile), extractedLSBI);

            System.out.println("Data extracted from " + bmpFile + " using LSBI method.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}