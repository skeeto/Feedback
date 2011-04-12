package feedback;

import javax.swing.JApplet;

public class Applet extends JApplet {
    private static final long serialVersionUID = 2788774940173059916L;

    private Feedback feedback;

    public void init() {
        feedback = new Feedback();
        add(feedback);
        (new Thread(feedback)).start();
        feedback.pause(true);
    }

    public void start() {
        feedback.pause(false);
    }

    public void stop() {
        feedback.pause(true);
    }
}
