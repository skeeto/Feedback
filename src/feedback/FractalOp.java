package feedback;

import java.util.ArrayList;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/* FractalOp -- Performs one step of fractalizing an image.
 *
 * This isn't really an honest BufferedImageOp implementation. It's
 * just enough for my needs.
 */
public class FractalOp implements BufferedImageOp {

    private ArrayList<Rectangle2D.Double> screens;

    public FractalOp(boolean random) {
        screens = new ArrayList<Rectangle2D.Double>();
        if (random) {
            int count = (int) Math.random() * 8 + 3;
            for (int i = 0; i < count; i++) {
                double s = Math.random();
                Rectangle2D.Double r;
                r = new Rectangle2D.Double(Math.random(), Math.random(), s, s);
                screens.add(r);
            }
        } else {
            screens.add(new Rectangle2D.Double(0,    0,   0.5, 0.5));
            screens.add(new Rectangle2D.Double(0.5,  0,   0.5, 0.5));
            screens.add(new Rectangle2D.Double(0.25, 0.5, 0.5, 0.5));
        }
    }

    public BufferedImage createCompatibleDestImage(BufferedImage src,
            ColorModel destCM) {
        return null;
    }

    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        Graphics2D g = dst.createGraphics();
        int w = src.getWidth();
        int h = src.getHeight();
        g.clearRect(0, 0, w, h);
        for (Rectangle2D.Double r : screens) {
            g.drawImage(src, (int) (r.getX() * w), (int) (r.getY() * h),
                        (int) (r.getWidth() * w),
                        (int) (r.getHeight() * h), null);
        }
        return dst;
    }

    public RenderingHints getRenderingHints() {
        return null;
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return null;
    }

    public Rectangle2D getBounds2D(BufferedImage src) {
        return null;
    }
}
