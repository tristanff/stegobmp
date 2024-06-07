import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BMPReader {
    public static void main(String[] args) {
        try {
            File bmpFile = new File("path/to/your/image.bmp");
            BufferedImage image = ImageIO.read(bmpFile);

            System.out.println("Width: " + image.getWidth());
            System.out.println("Height: " + image.getHeight());

            javax.swing.JFrame frame = new javax.swing.JFrame();
            frame.getContentPane().add(new javax.swing.JLabel(new javax.swing.ImageIcon(image)));
            frame.pack();
            frame.setVisible(true);
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        } catch (IOException e) {
            System.err.println("Error reading BMP file: " + e.getMessage());
        }
    }
}