package feedback;

import java.util.Random;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;
import java.util.ArrayList;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageOp;
import java.awt.image.Kernel;
import java.awt.image.ConvolveOp;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;
import javax.swing.JFrame;

public class Feedback extends JPanel implements Runnable {
    private static final long serialVersionUID = 328407319480529736L;

    public static int WIDTH = 640;
    public static int HEIGHT = 640;
    public static int SPEED = 100;
    public static double ANGLE = 0.5;
    public static int BLUR = 1;

    private BufferedImage image;
    private ArrayList<BufferedImageOp> ops;
    private Random rng;

    public static void main(final String[] args) {
        JFrame frame = new JFrame("Feedback");
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

        ops = new ArrayList<BufferedImageOp>();
        AffineTransform rotate = new AffineTransform();
        rotate.rotate(ANGLE, WIDTH / 2, HEIGHT / 2);
        ops.add(new AffineTransformOp(rotate, AffineTransformOp.TYPE_BILINEAR));
        ops.add(getGaussianBlurFilter(BLUR, true));
        ops.add(getGaussianBlurFilter(BLUR, false));

        rng = new Random();

        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.RED);
        g.fillOval(100, 200, 50, 50);
    }

    private void iterate() {
        BufferedImage last = deepCopy(image);
        for (BufferedImageOp op : ops) {
            last = op.filter(last, null);
        }
        image.getGraphics().drawImage(last, 0, 0, this);

        /* Disturb at random. */
        if (rng.nextInt(20) == 1) {
            Graphics g = image.getGraphics();
            int r = (int) Math.abs(rng.nextGaussian() * 100);
            int y = rng.nextInt(WIDTH);
            int x = rng.nextInt(HEIGHT);
            g.setColor(new Color(rng.nextInt(256), rng.nextInt(256),
                                 rng.nextInt(256), rng.nextInt(256)));
            g.fillOval(x, y, r, r);
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
        g.drawImage(image, 0, 0, this);
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
