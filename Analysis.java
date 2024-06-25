import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Analysis {

    public static void main(String[] args) {
        String bmpFile = "data/paris.bmp";

            BMPReader bmpReader = new BMPReader();
            byte[] bmpData = bmpReader.readImage(bmpFile);

            // Attempt to extract data using the LSBI method
            byte[] extractedLSBI = StegoImage.steganalisisLSBI(bmpData);

            // Find the position of the first ASCII code for '.'
            int dotIndex = -1;
            /*for (int i = 0; i < extractedLSBI.length - 1; i++) {
                if (extractedLSBI[i] == 46) { // ASCII code for '.'
                    dotIndex = i;
                    break;
                }
            }*/

            for (int i = 1; extractedLSBI.length - i > 1; i++) {
                if (extractedLSBI[extractedLSBI.length - i] == 46) {
                    dotIndex = extractedLSBI.length - i;
                    break;
                }
            }

            byte[] contenido = new byte[dotIndex];
            System.arraycopy(extractedLSBI, 0, contenido, 0, dotIndex);

            System.out.println(extractedLSBI[dotIndex]);
            String filename;

            if (dotIndex != -1 && dotIndex < extractedLSBI.length - 1) {
                // Extract the file extension assuming it follows the dot
                byte[] extensionBytes = new byte[extractedLSBI.length - dotIndex - 1];
                System.arraycopy(extractedLSBI, dotIndex, extensionBytes, 0, extensionBytes.length);
                String fileExtension = new String(extensionBytes);
                String name = "Output";
                filename = name.concat(fileExtension);

                // Print or use the file extension
                System.out.println("File extension : "+ fileExtension);
            } else {
                System.err.println("File type not found in " + bmpFile);
                filename = "Wrong";
            }

            //String contenidoF = new String(contenido);
            try {
                System.out.println(filename);
                Path path = Paths.get(filename);
                Files.write(path, contenido);
            } catch (IOException e) {
                e.printStackTrace();
            }
           

            // Save the extracted data to a file for examination
            //Files.write(Paths.get("test1_" + bmpFile), extractedLSBI);

            System.out.println("Data extracted from " + bmpFile + " using LSBI method.");
    }
}