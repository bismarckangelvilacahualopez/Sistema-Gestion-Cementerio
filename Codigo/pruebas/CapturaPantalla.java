import cementerio.Main;
import java.lang.reflect.Constructor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class CapturaPantalla {
  public static void main(String[] args) throws Exception {
    final Main[] frame = new Main[1];
    SwingUtilities.invokeAndWait(()->{
      try {Constructor<Main> c=Main.class.getDeclaredConstructor();c.setAccessible(true);frame[0]=c.newInstance();frame[0].setSize(1280,800);frame[0].setVisible(true);}catch(Exception e){throw new RuntimeException(e);}
    });
    Thread.sleep(1400);
    BufferedImage img = new Robot().createScreenCapture(new Rectangle(0,0,1280,800));
    ImageIO.write(img,"png",new File(args[0]));
    SwingUtilities.invokeAndWait(()->frame[0].dispose());
  }
}
