import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class BMPReader {

    public static int getPixelOffset(byte[] infile, int header_offset){ // 4 Byte Header Information
        int offset = 0;
        for (int i = 3; i >= 0; i--) {
            offset |= (infile[i + header_offset] & 0xFF);
            if(i>0){
                offset <<= 8;
            }
        }
        return offset;
    }

    public static int getLowerPixelOffset(byte[] infile, int header_offset){ // 2 Byte Header Information
        int offset = 0;
        for (int i = 1; i >= 0; i--) {
            offset |= (infile[i + header_offset] & 0xFF);
            if(i>0){
                offset <<= 8;
            }
        }
        return offset;
    }

    public byte[] readImage(String IOpath) {

        File file = new File(IOpath);
        byte[] fileContent = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            // Read the BMP file into the byte array
            fis.read(fileContent);

            // Output the size of the byte array
            System.out.println("File size in bytes: " + fileContent.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total Length of Header: " + getPixelOffset(fileContent, 10)); // Lenght of Header
        System.out.println("Total Length of DIB Header: " + getPixelOffset(fileContent, 14)); // Lenght of DIB Header
        System.out.println("Total Width of Image: " + getPixelOffset(fileContent, 18)); // Width of Image
        System.out.println("Total Height of Image: " + getPixelOffset(fileContent, 22)); // Height of Image
        System.out.println("Nº Color Panes (Must be 1): " + getLowerPixelOffset(fileContent, 26)); // Nro of Color Panes (Must be 1)
        System.out.println("Nº of bits per Pixel: " + getLowerPixelOffset(fileContent, 28)); //Bits per pixel
        System.out.println("Compression Mode (0 is uncompressed): " + getPixelOffset(fileContent, 30)); // Compression: 0 for uncompressed
        System.out.println("Raw size of file without Headers: " + getPixelOffset(fileContent, 34)); // Size of raw bitmap data including padding without header

        return fileContent;
    }

    public int getOffset(byte[] bmpFile){
        return getPixelOffset(bmpFile, 10);
    }

   /* public static void main(String[] args) {
        String image = "image1.bmp";
        BMPReader reader1 = new BMPReader();
        byte[] contenido;
        contenido = reader1.readImage(image);

        System.out.println("The file succesfully loaded");
    }*/

}
