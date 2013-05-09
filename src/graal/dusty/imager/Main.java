package graal.dusty.imager;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

public class Main implements ActionListener,MouseListener,KeyListener {
  boolean key_cooloff = false;
  boolean isbody      = false;
  int oldcolor        = -1;
  Color softblack     = new Color(5,5,5,255);
  Color softwhite     = new Color(250,250,250,255);
  ArrayList<Integer> palette = new ArrayList<Integer>();
   
  JFrame frame,options;
  JButton saveButton;
  JLabel bgLabel  = new JLabel("");
  JLabel picLabel = new JLabel("");
  JFileChooser fc = new JFileChooser();
  BufferedImage saveimg = new BufferedImage(1,1,BufferedImage.TYPE_4BYTE_ABGR);
  
  Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
  int screenwidth = (int)screenSize.getWidth();
  int screenheight = (int)screenSize.getHeight();

  public static void main(String[] args) throws IOException {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ex) {
    }
    new Main();
  }

  public Main() throws IOException {
    makeFrame();
  }
  
  private void makeFrame() throws IOException {
    String desktop = System.getProperty("user.home") + "\\Desktop\\";
    fc.setCurrentDirectory(new File(desktop));
    
    frame = new JFrame("Graal Image Helper");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);

    updateImage(getImgfromFile("res/hat_template.png"));
  }
  
  // UPDATE PANEL WITH BUFFERED IMAGE FROM FILE
  public void updateImage(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    
    picLabel.setIcon(new ImageIcon(img));
    
    if (w == 128 && h == 720) {
      img = TransformColorToNewColor(img,new Color(0xfffffff7),new Color(255,255,255,255));
    }
    
    getPalette(img);

    // IF THE IMAGE IS A BODY, 
    if (w == 128 && h == 720) {
      isbody = true;
      saveimg = img;
      updateImagePanel(w,h);
      saveFile();
    } else {
      isbody = false;
      img = TransformColorToNewColor(img,new Color(0,0,0,255),softblack);
      img = TransformColorToNewColor(img,new Color(255,255,255,255),softwhite);
      palette.add(softwhite.getRGB());
      palette.add(softblack.getRGB());
      saveimg = img;
    }
    
    /*
    int white = new Color(255,255,255,255).getRGB();
    int black = new Color(0,0,0,255).getRGB();
    
    for (int i=0;i<h;i++) {
      for (int j=0;j<w;j++) {
        if (img.getRGB(j,i) == white) img.setRGB(j,i,new Color(250,250,250,255).getRGB());
        else if (img.getRGB(j,i) == black) img.setRGB(j,i,new Color(5,5,5,255).getRGB());
      }
    }
    */
    
    updateImagePanel(w,h);
  }
  
  // MAP THE PALETTE OF THE IMAGE
  public void getPalette(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    int trans = new Color(255,0,255,0).getRGB();

    palette.clear();
    // IF BODY IS DETECTED ADD VANILLA COLORS, OTHERWISE ADD TRANSPARENCY
    if (w == 128 && h == 720) {
      int[] colorMap = {0xff000000,0xff0000ff,img.getRGB(0,0),0xffce1829,0xffff0000,0xffffad6b,0xffffffff};
      for (int i : colorMap) palette.add(i);
    } else palette.add(new Color(255,0,255,0).getRGB());
    
    for (int i=0;i<h;i++) {
      for (int j=0;j<w;j++) {
        int rgb = img.getRGB(j,i);

        if (palette.contains(rgb) || rgb == trans) continue; // IF COLOR ALREADY PRESENT, OR TRANSPARENT SKIP
        palette.add(rgb);
      }
    }
  }
  
  public boolean isVanillaBody() {
    int[] colorMap = {0xff000000,0xff0000ff,0xff008400,0xffce1829,0xffff0000,0xffffad6b,0xffffffff};
   
    if (palette.size() > 8) return false;
    
    for (int i : colorMap) {
      echo("" + i);
      if (!palette.contains(i)) {
        echo("Fail!");
        return false;
      }
    }
    
    return true;
  }
  
  // RETURNS
  public void echoColor(int rgb) {
    int a = (rgb >> 24) & 0xff;
    int r = (rgb >> 16) & 0xff;
    int g = (rgb >> 8) & 0xff;
    int b = rgb & 0xff;
    
    echo("#" + Integer.toHexString(rgb) + ": " + r + "," + g + "," + b + "," + a);
  }

  // UPDATE PANEL WITH NEW IMAGE INFO
  public void updateImagePanel(int w,int h) {
    frame.getContentPane().add(picLabel);
    
    updateBG(w,h);
   
    picLabel.setBounds(0,0,w,h);
    frame.pack();
    picLabel.setBounds(0,0,w,h);
    
    frame.setVisible(true);
    frame.setLocationRelativeTo(null);
    picLabel.setFocusable(true);
    picLabel.requestFocusInWindow();
    picLabel.addKeyListener(this);
    picLabel.addMouseListener(this);
    picLabel.requestFocus();
    
    // WAS GOING TO JUST SIZE DOWN LARGE IMAGES BECAUSE THE PAINTING BROKE
    // IT ENDED UP FIXING THE BUG ANYWAYS!
    if (w > 800) {
      frame.setSize(800,h);
    }
    frame.pack();
  }
  
  public void updateBG(int w,int h) {
    BufferedImage bg;
    bg = new BufferedImage(w,h,BufferedImage.TYPE_3BYTE_BGR);
    try {

      BufferedImage bgtile = getImgfromFile("res/panebg.png");
      int countx = (int)(w/256)+1;
      int county = (int)(h/256)+1;
      
      for (int i=0;i<county;i++) {
        for (int j=0;j<countx;j++) {
          bg.getGraphics().drawImage(bgtile,j*256,i*256,null);
        }
      }
      
      bgLabel.setIcon(new ImageIcon(bg));
      frame.getContentPane().add(bgLabel);
      frame.pack();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void keyTyped(KeyEvent e) {
  }
  
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    if (keyCode == 17) return;
    //echo("" + e.getKeyCode() + " : " + e.getModifiers());
   
    if (key_cooloff == true) return;
    key_cooloff = true;
    
    // GET CTRL+V AND IMPORT CLIPBARD
    if (keyCode == 86 && e.getModifiers() == 2) {
      BufferedImage img = getImageFromClipboard();
      if (img != null) updateImage(img);
    // GET CTRL+S AND PROMPT SAVE
    } else if (keyCode == 83 && e.getModifiers() == 2) {
      saveFile();
    }
  }
  
  public void saveFile() {
    int returnVal = fc.showSaveDialog(frame);
    
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      if (!file.getName().endsWith(".png")) {
        file = new File(file.getPath() + ".png");
      }
      try { 
        if (isbody) {
          //int palettesize = palette.size() > 253 ? 253 : palette.size();
          for (int i : palette) echoColor(i);
          int palettesize = 255;
          int[] colorMap = new int[palettesize];   
          Arrays.fill(colorMap,0xffff00ff);
          Iterator<Integer> iterator = palette.iterator();
          for (int i=0;i<palette.size();i++) {
            if (i > palettesize-1) break;
            colorMap[i] = iterator.next().intValue(); 
          }
          
          IndexColorModel cm = new IndexColorModel(8,colorMap.length,colorMap,0,true,-1,DataBuffer.TYPE_BYTE);
          BufferedImage indexedimg = new BufferedImage(saveimg.getWidth(),saveimg.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,cm);
          int[] test = saveimg.getRGB(0,0,saveimg.getWidth(),saveimg.getHeight(),(int[])null,0,saveimg.getHeight());
          indexedimg.setRGB(0,0,saveimg.getWidth(),saveimg.getHeight(),test,0,saveimg.getHeight());
          
          ImageIO.write(indexedimg, "png", file);
        } else {
          String indexedfile = file.getAbsolutePath().replaceFirst(".png","");
          indexedfile = indexedfile + "_offline" + ".png";
         
          File temp = File.createTempFile("graalimager", ".tmp"); 
          ImageIO.write(saveimg, "gif",temp);
          
          BufferedImage indexedimg = new BufferedImage(saveimg.getWidth(),saveimg.getHeight(),BufferedImage.TYPE_BYTE_INDEXED);
          indexedimg = ImageIO.read(temp);

          
          if (palette.size() > 255) {
            ImageIO.write(indexedimg, "png", new File(indexedfile));
            ImageIO.write(saveimg, "png", file);
          } else ImageIO.write(indexedimg, "png",file);
        }
        
        
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } else {
       // SAVE CANCELLED
    }
  }
  
  public void keyReleased(KeyEvent e) {
    key_cooloff = false;
  }
  
  public void actionPerformed(ActionEvent e) {
  }
    
  public void mouseEntered(MouseEvent e) {
  }
    
  public void action(ActionEvent e) {
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    if (isbody) return;
    if (e.getButton() == 1) picLabel.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
  }
    
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() != 1 || isbody) {
      picLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      return;
    }
    
    int mx = e.getX();
    int my = e.getY();
    int w  = saveimg.getWidth();
    int h  = saveimg.getHeight();
    
    // ONLY ALLOW ONE COLOR TO BE TRANSPARENT. REPLACE OLD TRANSPARENCY WITH OLD COLOR
    /*
    if (oldcolor != -1) {
      for (int i=0;i<h;i++) {
        for (int j=0;j<w;j++) {
          if (saveimg.getRGB(j,i) == 0) saveimg.setRGB(j,i,oldcolor);
        }
      }
    }
    */

    saveimg = TransformColorToNewColor(saveimg,new Color(saveimg.getRGB(mx,my)),new Color(255,0,255,0));
    /*
    int dropcolor = oldcolor = saveimg.getRGB(mx,my);
    
    int a = (dropcolor >> 24) & 0xff;
    
    if (a < 255) {
      picLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      return;
    }
    
    for (int i=0;i<h;i++) {
      for (int j=0;j<w;j++) {
        if (saveimg.getRGB(j,i) == dropcolor) saveimg.setRGB(j,i,new Color(255,0,255,0).getRGB());
      }
    }
    */
    
    getPalette(saveimg);
    picLabel.setIcon(new ImageIcon(saveimg));
    picLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    
    picLabel.revalidate();
    picLabel.repaint();
    frame.revalidate();
    frame.repaint();
  }
  
  public void echo(String s) {
    System.out.println(s); 
  }
    
  // GET IMAGE FROM FILE
  public BufferedImage getImgfromFile(String s) throws IOException {
    BufferedImage importimg = null;
    
    // IMPORT IMAGE FROM FILE AND TRANSFER IT INTO NEW IMAGE WITH ALPHA SUPPORT
    //importimg = ImageIO.read(new File(s));
    URL u = this.getClass().getClassLoader().getResource(s);
    
    importimg = ImageIO.read(u);
    
    BufferedImage img = new BufferedImage(importimg.getWidth(),importimg.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
    img.getGraphics().drawImage(importimg, 0, 0, null);
    return img;
    
  }
  
  // GET IMAGE FROM CLIPBOARD
  // http://alvinalexander.com/blog/post/jfc-swing/how-copy-paste-image-into-java-swing-application
  public BufferedImage getImageFromClipboard() {
    Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    
    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      try {
        // IMPORT IMAGE FROM CLIPBOARD AND TRANSFER IT INTO NEW IMAGE WITH ALPHA SUPPORT
        BufferedImage importimg = (BufferedImage) transferable.getTransferData(DataFlavor.imageFlavor);
        BufferedImage img = new BufferedImage(importimg.getWidth(),importimg.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
        img.getGraphics().drawImage(importimg,0,0,null);
        
        return img;
      }
      catch (UnsupportedFlavorException e) {
        e.printStackTrace();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
  

  private BufferedImage TransformColorToNewColor(BufferedImage image, Color c1, Color c2) {
    // Primitive test, just an example
    final int r1 = c1.getRed();
    final int g1 = c1.getGreen();
    final int b1 = c1.getBlue();
    final int r2 = c2.getRed();
    final int g2 = c2.getGreen();
    final int b2 = c2.getBlue();
    final int a2 = c2.getAlpha();
    ImageFilter filter = new RGBImageFilter() {
      public final int filterRGB(int x, int y, int argb) {
        int a = 255;
        if (a2 > 0) a = (argb & 0xFF000000) >> 24;
        else a = 0;
        int r = (argb & 0xFF0000) >> 16;
        int g = (argb & 0xFF00) >> 8;
        int b = (argb & 0xFF);

        // Check if this color matches c1.  If not, it is not our target color.
        // Don't bother with it in this case.
        if (r != r1 || g != g1 || b != b1)
          return argb;

        // Set r, g, and b to our new color.  Bit-shift everything left to get it
        // ready for re-packing.
        if (a2 > 0) a = a << 24;
        r = r2 << 16;
        g = g2 << 8;
        b = b2;

        // Re-pack our colors together with a bitwise OR.
        //return a | r | g | b;
        return a | r | g | b;
      }
    };

    ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
    Image new_renderimage = Toolkit.getDefaultToolkit().createImage(ip);
    BufferedImage new_renderbuffer = convertImagetoBuffered(new_renderimage);
    if (new_renderbuffer == null) return null;
    return new_renderbuffer;
  }
  
  public static BufferedImage convertImagetoBuffered(Image image) {
    if (image instanceof BufferedImage) return (BufferedImage)image;

    // This code ensures that all the pixels in the image are loaded
    image = new ImageIcon(image).getImage();

    // Determine if the image has transparent pixels; for this method's
    // implementation, see Determining If an Image Has Transparent Pixels
    boolean hasAlpha = true;

    // Create a buffered image with a format that's compatible with the screen
    BufferedImage bimage = null;
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    try {
      // Determine the type of transparency of the new buffered image
      int transparency = Transparency.OPAQUE;
      if (hasAlpha) transparency = Transparency.BITMASK;

      // Create the buffered image
      GraphicsDevice gs = ge.getDefaultScreenDevice();
      GraphicsConfiguration gc = gs.getDefaultConfiguration();
      bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
    } catch (HeadlessException e) {
      // The system does not have a screen
    }

    if (bimage == null) {
      // Create a buffered image using the default color model
      int type = BufferedImage.TYPE_INT_RGB;
      if (hasAlpha) type = BufferedImage.TYPE_INT_ARGB;
      bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
    }

    // Copy image to buffered image
    Graphics g = bimage.createGraphics();

    // Paint the image onto the buffered image
    g.drawImage(image, 0, 0, null);
    g.dispose();

    return bimage;
  }
}
