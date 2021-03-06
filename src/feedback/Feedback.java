package feedback;

import java.io.File;

import java.util.Random;
import java.util.ArrayList;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImageOp;
import java.awt.image.AffineTransformOp;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.imageio.ImageIO;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/* Simulates a video camera feedback look. The mouse draws onto the
 * screen interactively, in addition to random disturbances, and the
 * screen is distorted and fed back into itself over and over. */
public class Feedback extends JPanel implements Runnable {
    private static final long serialVersionUID = 328407319480529736L;

    /* Default settings */
    public static int WIDTH = 640;       /* Display width */
    public static int HEIGHT = 640;      /* Display height */
    public static int SPEED = 30;        /* Animation delay (ms) */
    public static double ANGLE = 2.5;    /* Rotate op (radians) */
    public static double SCALE = 0.99;   /* Resize op */
    public static int BLUR = 1;          /* Gaussian filter radius */
    public static int INIT_DISTURB = 50; /* Initial number of disturbs */
    public static int REINIT = 200;      /* Reinit period (steps) */
    public static int M_SIZE = 50;       /* Mouse pointer size */
    public static int COLOR_SPEED = 15;  /* Mouse color change speed */
    public static float ENHANCE = 1.5f;  /* Display color enhancement */

    /* Image operators */
    private ArrayList<BufferedImageOp> ops;
    private RescaleOp display;   /* Display purpose only. */

    /* State */
    private BufferedImage image, workA, workB;
    private int counter = 1;
    private Random rng;
    private boolean mouse;
    private int mX, mY;
    private int mR, mG, mB, mA;

    /* GUI */
    public static JFrame frame;

    /* Config */
    private boolean pause = false;
    private boolean random = true;
    private boolean help = false;
    private double angle = ANGLE;
    private double scale = SCALE;
    private volatile double speed = SPEED;

    /* Create a frame and stick a Feedback panel in it. */
    public static void main(final String[] args) {
        frame = new JFrame("Feedback");
        Feedback feedback = new Feedback();
        frame.add(feedback);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

        (new Thread(feedback)).start();
    }

    public Feedback() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        display = new RescaleOp(ENHANCE, 0.0f, null);

        createOps();

        rng = new Random();

        GraphicsEnvironment ge;
        ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getScreenDevices()[0];
        GraphicsConfiguration gc = gd.getConfigurations()[0];
        image = gc.createCompatibleImage(WIDTH, HEIGHT);
        workA = gc.createCompatibleImage(WIDTH, HEIGHT);
        workB = gc.createCompatibleImage(WIDTH, HEIGHT);
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.BLACK);
        clear();

        /* Set up mouse interaction. */
        this.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                mX = e.getX();
                mY = e.getY();
                if (!pause)
                    mouse(false);
            }
        });
        this.addMouseListener(new MouseListener() {
            public void mouseEntered(MouseEvent e) {
                mouse = true;
            }
            public void mouseExited(MouseEvent e) {
                mouse = false;
            }
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                requestFocus();
            }
            public void mousePressed(MouseEvent e) {
            }
            public void mouseReleased(MouseEvent e) {
            }
        });

        /* Set up keyboard interaction. */
        this.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                switch (e.getKeyChar()) {
                case 's':
                    screenshot();
                    break;
                case 'n':
                    random ^= true;
                    if (random)
                        message("Noise on");
                    else
                        message("Noise off");
                    break;

                    /* Rotation*/
                case 'r':
                    angle /= 1.01;
                    createOps();
                    break;
                case 'R':
                    angle *= 1.01;
                    createOps();
                    break;

                    /* Scale */
                case 'g':
                    scale /= 1.01;
                    createOps();
                    break;
                case 'G':
                    scale *= 1.01;
                    scale = Math.min(scale, SCALE);
                    createOps();
                    break;

                    /* Pause/play */
                case 'p':
                    pause(null);
                    break;

                    /* Clear the screen */
                case 'c':
                    clear();
                    break;

                    /* Animation speed */
                case '+':
                    speed /= 1.1;
                    break;
                case '-':
                    speed *= 1.1;
                    break;
                }
            }
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F1
                || e.getKeyChar() == 'h') {
                    help = true;
                    repaint();
                }
            }
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F1
                || e.getKeyChar() == 'h') {
                    help = false;
                    repaint();
                }
            }
        });
        requestFocusInWindow();
        requestFocus();

        mR = rng.nextInt(256);
        mG = rng.nextInt(256);
        mB = rng.nextInt(256);
        mA = 255;

        initDisturb();
    }

    /* Pause/play the simulation. */
    public synchronized void pause(Boolean pause) {
        if (pause != null && this.pause == pause)
            return;
        synchronized (image) {
            image.notifyAll();
        }
        this.pause ^= true;
    }

    /* Reset the image to blank. */
    public void clear() {
        synchronized (image) {
            image.getGraphics().clearRect(0, 0, WIDTH, HEIGHT);
        }
    }

    /* Reinitialize the image operators using the current parameters. */
    private synchronized void createOps() {
        ArrayList<BufferedImageOp> ops = new ArrayList<BufferedImageOp>();
        AffineTransform affine = new AffineTransform();
        affine.rotate(angle, WIDTH / 2, HEIGHT / 2);
        affine.scale(scale, scale);
        ops.add(new AffineTransformOp(affine, AffineTransformOp.TYPE_BILINEAR));
        this.ops = ops;
    }

    /* Draw the mouse circle cursor to the image. */
    private void mouse(boolean change) {
        synchronized (image) {
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(mR, mG, mB, mA));
            g.fillOval(mX - M_SIZE / 2, mY - M_SIZE / 2, M_SIZE, M_SIZE);
            if (change) {
                mR += rng.nextGaussian() * COLOR_SPEED;
                mG += rng.nextGaussian() * COLOR_SPEED;
                mB += rng.nextGaussian() * COLOR_SPEED;
                mA += rng.nextGaussian();
                mR = Math.max(0, Math.min(mR, 255));
                mG = Math.max(0, Math.min(mG, 255));
                mB = Math.max(0, Math.min(mB, 255));
                mA = Math.max(128, Math.min(mA, 255));
            }
        }
        repaint();
    }

    /* Open a dialog to save the current image. */
    private void screenshot() {
        boolean state = pause;
        pause(true);
        try {
            JFileChooser fc = new JFileChooser();
            FileNameExtensionFilter filter
            = new FileNameExtensionFilter("PNG Images", "png");
            fc.setFileFilter(filter);
            int rc = fc.showDialog(frame, "Save Screenshot");
            if (rc == JFileChooser.APPROVE_OPTION) {
                save(fc.getSelectedFile());
            }
        } catch (java.security.AccessControlException ec) {
            /* We're in the applet. */
            System.out.println("Cannot save screenshot now.");
        }
        pause(state);
    }

    /* Save the current image to the given file. */
    private void save(File file) {
        try {
            ImageIO.write(display.filter(image, null), "PNG", file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                                          "Unable to write " + file,
                                          "Save failed",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /* Display a message to the user via the image. */
    private void message(String msg) {
        synchronized (image) {
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(msg);
            g.drawString(msg, WIDTH / 2 - w / 2, HEIGHT - fm.getAscent() * 2);
        }
        repaint();
    }

    /* Iterate the image by one step. */
    private void iterate() {
        if (!mouse)
            counter++;

        synchronized (image) {
            /* Apply "camera" effects to the image. */
            ops.get(0).filter(image, workA);
            for (int i = 1; i < ops.size(); i++) {
                ops.get(i).filter(workA, workB);
                BufferedImage swap = workA;
                workA = workB;
                workB = swap;
            }

            /* Mix back into the original image. */
            BufferedImage last = workA;
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    int a = last.getRGB(x, y);
                    int b = image.getRGB(x, y);
                    int r = 0;
                    for (int i = 0; i < 32; i += 8) {
                        int va = (a >> i) & 0xFF;
                        int vb = (b >> i) & 0xFF;
                        /* screen */
                        //int vr = 255 - ((255 - va) * (255 - vb)) / 255;
                        /* average */
                        int vr = (va + vb) / 2;
                        r = r | (vr << i);
                    }
                    image.setRGB(x, y, r);
                }
            }
        }

        /* Apply mouse input. */
        if (mouse) {
            mouse(true);
        }

        /* Disturb at random. */
        if (random) {
            if (rng.nextInt(5) == 0) {
                disturb();
            } else if (rng.nextInt(counter) > REINIT) {
                initDisturb();
                counter = 1;
            }
        }
    }

    /* Add a disturbance object to the image (circle or square of
     * random color and size). */
    private void disturb() {
        synchronized (image) {
            Graphics g = image.getGraphics();
            int r = (int) Math.abs(rng.nextGaussian() * 100);
            int y = rng.nextInt(WIDTH);
            int x = rng.nextInt(HEIGHT);
            g.setColor(new Color(rng.nextInt(256), rng.nextInt(256),
                                 rng.nextInt(256), rng.nextInt(127) + 128));
            switch (rng.nextInt(2)) {
            case 0:
                g.fillRect(x, y, r, r);
                break;
            case 1:
                g.fillOval(x, y, r, r);
                break;
            }
        }
    }

    /* Create the initial disturbance. */
    private void initDisturb() {
        for (int i = 0; i < INIT_DISTURB; i++) {
            disturb();
        }
    }

    /* The simulation thread. */
    public void run() {
        while (true) {
            try {
                Thread.sleep((int) speed);
                if (pause) {
                    synchronized (image) {
                        image.wait();
                    }
                }
                iterate();
                repaint();
            } catch (InterruptedException e) {
                /* Nothing. */
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (image) {
            g.drawImage(display.filter(image, null), 0, 0, this);
        }
        if (help) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            /* Print some help information. */
            FontMetrics fm = g.getFontMetrics();
            int h = fm.getAscent() + fm.getDescent();
            g.setColor(Color.WHITE);
            int x1 = 10;
            int x2 = 50;
            int y = h;
            g.drawString("Shortcut keys:", x1, y);
            y += h * 2;
            g.drawString("n", x1, y);
            g.drawString("Toggle automated noise", x2, y);
            y += h;
            g.drawString("g/G", x1, y);
            g.drawString("Increase/decrease gravity", x2, y);
            y += h;
            g.drawString("r/R", x1, y);
            g.drawString("Increase/decrease rotation", x2, y);
            y += h;
            g.drawString("+/-", x1, y);
            g.drawString("Increase/decrease animation speed", x2, y);
            y += h;
            g.drawString("c", x1, y);
            g.drawString("Clear the screen", x2, y);
            y += h;
            g.drawString("p", x1, y);
            g.drawString("Toggle pause/play", x2, y);
            y += h;
            g.drawString("s", x1, y);
            g.drawString("Save a screenshot", x2, y);
            y += h;
        }
    }
}
