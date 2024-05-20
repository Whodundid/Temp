package controller.globe;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import eutil.datatypes.util.EList;
import eutil.math.ENumUtil;

public class RenderingPanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    
    //========
    // Fields
    //========
    
    private Camera camera;
    private PerspectiveRenderer renderer;
    private BufferedImage img;
    
    private EList<Entity> entities = EList.newList();
    
    private boolean lockCursor = false;
    private Robot mouseMover;
    private Cursor standard;
    private Cursor hidden;
    
    private int imgWidth = 320;
    private int imgHeight = 240;
    
    //==============
    // Constructors
    //==============
    
    public RenderingPanel(int width, int height) {
        imgWidth = width;
        imgHeight = height;
        
        camera = new Camera();
        renderer = new PerspectiveRenderer(width, height);
        
        // cursor stuff
        standard = getCursor();
        BufferedImage hiddenCursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        hidden = Toolkit.getDefaultToolkit().createCustomCursor(hiddenCursor, new Point(0, 0), "hidden");
        
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        
        try {
            mouseMover = new Robot();
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        
        // draw black background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // enable anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // setup image and render
        createPanelImage();
        renderer.render(camera, entities, img);
        
        // draw debug camera position and rotation
        g2.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, imgWidth, imgHeight, null);
        g2.setColor(Color.WHITE);
        String pos = "POS: " + camera.position;
        var posGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), pos);
        g2.drawString(pos, 0, (int) posGV.getVisualBounds().getHeight());
        String rot = "ROT: " + camera.rotation;
        var rotGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), rot);
        g2.drawString(rot, 0, (int) rotGV.getVisualBounds().getHeight() * 2);
        String fov = "FOV: " + renderer.getFOV();
        var fovGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), fov);
        g2.drawString(fov, 0, (int) fovGV.getVisualBounds().getHeight() * 4);
        
        // draw crosshair
        int midX = getWidth() / 2;
        int midY = getHeight() / 2;
        g2.setColor(Color.RED);
        g2.fillRect(midX - 4, midY, 10, 2);
        g2.fillRect(midX, midY - 4, 2, 10);
        
//        g2.setColor(Color.RED);
//        Point2d a = new Point2d(400, 300);
//        Point2d b = new Point2d(500, 200);
//        Point2d c = new Point2d(600, 100);
//        Point2d d = new Point2d(700, 200);
//        Point2d e = new Point2d(800, 300);
//        Point2d f = new Point2d(900, 400);
//        Point2d h = new Point2d(1000, 500);
//        Point2d i = new Point2d(1100, 400);
//        Point2d j = new Point2d(1200, 300);
//        var line = new BezierLine(a, b, c, d, e, f, h, i, j);
//        //line.setDrawControlPoints(true);
//        //line.setDrawStepDots(true);
//        //line.setStepDotColor(Color.cyan);
//        //line.setStepDotSize(5);
//        line.setLineWidth(2);
//        line.draw(g2);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        camera.onKeyPressed(e);
        
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_T) {
            if (lockCursor) lockCursor(false);
        }
        
        repaint();
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!lockCursor) return;
        
        float dx = e.getX() - getWidth() / 2;
        float dy = e.getY() - getHeight() / 2;
        
        var loc = getLocationOnScreen();
        mouseMover.mouseMove(loc.x + getWidth() / 2,
                             loc.y + getHeight() / 2);
        
        dx /= 180.0;
        dy /= -180.0;
        
        float fovRatio = renderer.getFOV() / 120f;
        dx *= fovRatio;
        dy *= fovRatio;
        
        camera.updateLook(dy * 15f, dx * 15f, 0);
        repaint();
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        float fov = renderer.getFOV() + e.getWheelRotation() * 2f;
        fov = ENumUtil.clamp(fov, 1f, 120f);
        renderer.setFOV(fov);
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) lockCursor(true);
        if (e.getButton() == 2) {
            camera.reset();
            repaint();
        }
    }

    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    
    //=========
    // Methods
    //=========
    
    public void lockCursor(boolean val) {
        lockCursor = val;
        setCursorHidden(val);
    }
    public void setCursorHidden(boolean val) {
        // don't set it again if it's already set
        if (getCursor() == ((val) ? hidden : standard)) return;
        // set to appropriate value
        setCursor((val) ? hidden : standard);
    }
    
    public void setup() {
        entities.clear();
        
        //imgWidth = 320; imgHeight = 240;
        //imgWidth = 640; imgHeight = 480;
        imgWidth = 1080; imgHeight = 720;
        //imgWidth = 1920; imgHeight = 1080;
        
        Sphere starsModel = new Sphere(100000.0f, 10, 10, Test3DWindow.stars);
        starsModel.insideOut = true;
        starsModel.fullBright = true;
        Entity stars = new Entity("Stars", starsModel);
        stars.setRotationDegrees(-90f, 0, 0);
        stars.setPosition(0, 0, 0);
        
        Sphere planetModel = new Sphere(10.0f, 50, 50, Test3DWindow.worldBig);
        planetModel.fullBright = true;
        Entity planet = new Entity("Planet", planetModel);
        planet.setRotationDegrees(-90.0f, 180f, 0.0f);
        planet.setPosition(0, 0, 0);
        
        Model axisModel = Test3DWindow.loadModel("axis.obj");
        Model mountainsModel = Test3DWindow.loadModel("mountains.obj");
        Model teapotModel = Test3DWindow.loadModel("teapot.obj");
        
        Entity axis = new Entity("Axis", axisModel);
        Entity mountains = new Entity("Mountains", mountainsModel);
        Entity teapot = new Entity("Teapot", teapotModel);
        teapot.setPosition(10.0f, 0.0f, 0.0f);
        mountains.setPosition(0.0f, -50f, 0.0f);
        
        Cube cubeModel1 = new Cube(Test3DWindow.world);
        Cube cubeModel2 = new Cube();
        //cubeModel.setTexture(Test3DWindow.world);
        Entity cube1 = new Entity("Cube1", cubeModel1);
        Entity cube2 = new Entity("Cube2", cubeModel2);
        cube1.setPosition(20.0f, 0.0f, 20.0f);
        cube2.setPosition(20.0f, 0.0f, 15.0f);
        
        //Shape doom1 = load("doom_E1M1.obj");
        //addShape(doom1);
        
        addEntity(stars);
//        addEntity(axis);
        addEntity(planet);
//        addEntity(mountains);
//        addEntity(teapot);
//        addEntity(cube1);
//        addEntity(cube2);
        
        renderer.onScreenResized(imgWidth, imgHeight);
        
        repaint();
    }
    
    public void addEntity(Entity entity) {
        if (entity == null) return;
        entities.add(entity);
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    private void createPanelImage() {
        img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    }
    
}
