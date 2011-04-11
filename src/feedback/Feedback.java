package feedback;

import java.io.File;

import java.util.Random;
import java.util.ArrayList;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

public class Feedback extends JPanel implements Runnable {
    private static final long serialVersionUID = 328407319480529736L;

    public static int WIDTH = 640;       /* Display width */
    public static int HEIGHT = 640;      /* Display height */
    public static int SPEED = 30;        /* Animation delay (ms) */
    public static double ANGLE = 2.5;    /* Rotate op (radians) */
    public static double SCALE = 0.99;   /* Resize op */
    public static int BLUR = 1;          /* Guassian filter radius */
    public static int INIT_DISTURB = 50; /* Initial number of disturbs */
    public static int REINIT = 200;      /* Reinitize period (steps) */
    public static int M_SIZE = 50;       /* Mouse pointer size */
    public static int COLOR_SPEED = 15;  /* Mouse color change speed */
    public static float ENHANCE = 1.5f;  /* Display color enhancement */

    private ArrayList<BufferedImageOp> ops;
    private RescaleOp display;   /* Display purpose only. */

    /* State */
    private BufferedImage image;
    private int counter;
    private Random rng;
    private boolean mouse;
    private int mX, mY;
    private int mR, mG, mB, mA;

    /* GUI */
    public static JFrame frame;

    /* Config */
    private boolean random = true;

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

        ops = new ArrayList<BufferedImageOp>();
        AffineTransform affine = new AffineTransform();
        affine.rotate(ANGLE, WIDTH / 2, HEIGHT / 2);
        affine.scale(SCALE, SCALE);
        ops.add(new AffineTransformOp(affine, AffineTransformOp.TYPE_BILINEAR));
        ops.add(getGaussianBlurFilter(BLUR, true));
        ops.add(getGaussianBlurFilter(BLUR, false));

        rng = new Random();

        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, WIDTH, HEIGHT);

        /* Set up mouse interaction. */
        this.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                mX = e.getX();
                mY = e.getY();
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
                case 'r':
                    random ^= true;
                    break;
                }
            }
            public void keyPressed(KeyEvent e) {
            }
            public void keyReleased(KeyEvent e) {
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

    private void mouse(boolean change) {
        Graphics g = image.getGraphics();
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

    private void screenshot() {
        JFileChooser fc = new JFileChooser();
        int rc = fc.showDialog(frame, "Save Screenshot");
        if (rc == JFileChooser.APPROVE_OPTION) {
            save(fc.getSelectedFile());
        }
    }

    private void save(File file) {
        try {
            ImageIO.write(display.filter(image, null), "PNG", file);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(frame,
                                          "Unable to write " + file,
                                          "Save failed",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iterate() {
        if (!mouse)
            counter++;
        BufferedImage last = deepCopy(image);
        for (BufferedImageOp op : ops) {
            last = op.filter(last, null);
        }
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

        if (mouse) {
            mouse(true);
        }

        /* Disturb at random. */
        if (random) {
            if (rng.nextInt(5) == 0) {
                disturb();
            } else if (rng.nextInt(counter) > REINIT) {
                initDisturb();
                counter = 0;
            }
        }
    }

    private void disturb() {
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

    private void initDisturb() {
        for (int i = 0; i < INIT_DISTURB; i++) {
            disturb();
        }
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(SPEED);
                iterate();
                repaint();
            } catch (InterruptedException e) {
                /* Nothing. */
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(display.filter(image, null), 0, 0, this);
    }

    public static ConvolveOp getGaussianBlurFilter(int radius,
            boolean horizontal) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }

        int size = radius * 2 + 1;
        float[] data = new float[size];

        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare)
                          / sigmaRoot;
            total += data[index];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        Kernel kernel = null;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }
}
