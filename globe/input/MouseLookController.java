package controller.globe.input;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import controller.globe.Camera;
import eutil.swing.listeners.CombinedMouseListener;
import eutil.swing.listeners.SimpleKeyListener;

public final class MouseLookController implements CombinedMouseListener, SimpleKeyListener {
    
    //========
    // Fields
    //========
    
    private final JComponent panel;
    private final Camera camera;
    private final Robot robot;
    private Cursor hidden;
    
    private boolean grabbed;
    private Point centerOnScreen;
    private long lastWarpNs;
    private static final long WARP_IGNORE_WINDOW_NS = 6_000_000L; // ~6 ms
    
    public float rollPerWheelDeg = 2.0f; // optional: roll with wheel while grabbed
    
    //==============
    // Constructors
    //==============
    
    public MouseLookController(JComponent surface, Camera camera) {
        this.panel = surface;
        this.camera = camera;
        
        try {
            robot = new Robot();
        }
        catch (AWTException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        BufferedImage hiddenCursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        hidden = Toolkit.getDefaultToolkit().createCustomCursor(hiddenCursor, new Point(0, 0), "hidden");
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!grabbed) return;
        
        // Ignore synthetic move right after we warped the cursor
        long now = System.nanoTime();
        if (now - lastWarpNs < WARP_IGNORE_WINDOW_NS) return;
        
        if (centerOnScreen == null) {
            centerOnScreen = centerOf(panel);
            if (centerOnScreen == null) return;
        }
        
        Point p = e.getLocationOnScreen();
        int dx = p.x - centerOnScreen.x;
        int dy = p.y - centerOnScreen.y;
        
        if (dx != 0 || dy != 0) {
            camera.onMouseDelta(dx, dy);
            warpToCenter();
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        //mouseMoved(e);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (grabbed) release();
            else grab();
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_T) {
            release();
        }
    }
    
    //=========
    // Methods
    //=========
    
    /** Start capturing the mouse. */
    public void grab() {
        if (grabbed) return;
        grabbed = true;
        panel.requestFocusInWindow();
        setCursorHidden(true);
        centerOnScreen = centerOf(panel);
        warpToCenter();
    }
    
    /** Stop capturing the mouse. */
    public void release() {
        if (!grabbed) return;
        grabbed = false;
        panel.setCursor(Cursor.getDefaultCursor());
    }
    
    // If the component is resized or moved, recenter
    public void onSurfaceChanged() {
        if (grabbed) warpToCenter();
    }
    
    public void setCursorHidden(boolean val) {
        // don't set it again if it's already set
        if (panel.getCursor() == ((val) ? hidden : Cursor.getDefaultCursor())) return;
        // set to appropriate value
        panel.setCursor((val) ? hidden : Cursor.getDefaultCursor());
    }
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    private void warpToCenter() {
        centerOnScreen = centerOf(panel);
        if (centerOnScreen != null) {
            lastWarpNs = System.nanoTime();
            robot.mouseMove(centerOnScreen.x, centerOnScreen.y);
        }
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    private static Point centerOf(JComponent c) {
        try {
            Point p = c.getLocationOnScreen();
            return new Point(p.x + c.getWidth() / 2, p.y + c.getHeight() / 2);
        }
        catch (IllegalComponentStateException ex) {
            return null;
        }
    }
    
}
