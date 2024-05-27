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
    
    private int renderScale = 50;
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
        String quality = "Q: " + renderScale;
        var qGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), quality);
        g2.drawString(quality, 0, (int) qGV.getVisualBounds().getHeight() * 5);
        
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
//        line.setDrawControlPoints(true);
//        line.setDrawStepDots(true);
//        line.setStepDotColor(Color.cyan);
//        line.setStepDotSize(5);
//        line.setLineWidth(2);
//        line.draw(g2, 80);
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
        if (e.isControlDown()) {
            renderScale += e.getWheelRotation() * -1;
            renderScale = ENumUtil.clamp(renderScale, 1, 120);
            setup();
        }
        else {
            float scale = 1.00f;
            if (e.isAltDown()) scale = 0.1f;
            float fov = renderer.getFOV() + e.getWheelRotation() * scale;
            fov = ENumUtil.clamp(fov, 0.1f, 120f);
            renderer.setFOV(fov);
        }

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
    
    @SuppressWarnings("unused")
    public void setup() {
        entities.clear();
        
        imgWidth = 21 * renderScale;
        imgHeight = 9 * renderScale;
        
        //imgWidth = 256; imgHeight = 144;
        //imgWidth = 320; imgHeight = 240;
        //imgWidth = 480; imgHeight = 270; // 30
        //imgWidth = 640; imgHeight = 360; // 40
        //imgWidth = 800; imgHeight = 450; // 50
        
        //imgWidth = 1280; imgHeight = 720;
        //imgWidth = 1920; imgHeight = 1080;
        
        Sphere starsModel = new Sphere(200000.0f, 10, 10, Test3DWindow.stars);
        starsModel.insideOut = true;
        starsModel.fullBright = true;
        Entity stars = new Entity("Stars", starsModel);
        stars.setRotationDegrees(-90f, 0, 0);
        stars.setPosition(0, 0, 0);
        
        Sphere planetModel = new Sphere(1.0f, 70, 70, Test3DWindow.worldBig);
        //Sphere planetModel = new Sphere(10.0f, 50, 50, null);
        planetModel.fullBright = true;
        Entity earth = new Entity("Planet", planetModel);
        earth.setRotationDegrees(-90.0f, 180f, 0.0f);
        
        
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
        
        Line3D line11 = new Line3D();
        line11.addPoint(7.25f, 0.0f, 7.25f);
        line11.addPoint(10.5f, 0.5f, 11.5f);
        line11.addPoint(11.0f, 2.0f, 12.0f);
        line11.lineColor = Color.GREEN;
        Entity line1 = new Entity("Line1", line11);
        line11.antiAlias = true;
        line11.lineWidth = 1;
        
        //Shape doom1 = load("doom_E1M1.obj");
        //addShape(doom1);
        
//        Sphere sunModel = new Sphere(1.0f, 70, 70, Test3DWindow.sun);
//        Sphere mercuryModel = new Sphere(1.0f, 70, 70, Test3DWindow.mercury);
//        Sphere venusModel = new Sphere(1.0f, 70, 70, Test3DWindow.venus);
//        Sphere marsModel = new Sphere(1.0f, 70, 70, Test3DWindow.mars);
//        Sphere jupiterModel = new Sphere(1.0f, 70, 70, Test3DWindow.jupiter);
//        Sphere saturnModel = new Sphere(1.0f, 70, 70, Test3DWindow.saturn);
//        Sphere uranusModel = new Sphere(1.0f, 70, 70, Test3DWindow.uranus);
//        Sphere neptuneModel = new Sphere(1.0f, 70, 70, Test3DWindow.neptune);
//        Sphere moonModel = new Sphere(1.0f, 70, 70, Test3DWindow.moon);
        
//        Entity sun = new Entity("sun", sunModel);
//        Entity mercury = new Entity("mercury", mercuryModel);
//        Entity venus = new Entity("venus", venusModel);
//        Entity mars = new Entity("mars", marsModel);
//        Entity jupiter = new Entity("jupiter", jupiterModel);
//        Entity saturn = new Entity("saturn", saturnModel);
//        Entity uranus = new Entity("uranus", uranusModel);
//        Entity neptune = new Entity("neptune", neptuneModel);
//        Entity moon = new Entity("moon", moonModel);
        
//        sun.setRotationDegrees(-90f, 0, 0);
//        mercury.setRotationDegrees(-90f, 0, 0);
//        venus.setRotationDegrees(-90f, 0, 0);
//        mars.setRotationDegrees(-90f, 0, 0);
//        jupiter.setRotationDegrees(-90f, 0, 0);
//        saturn.setRotationDegrees(-90f, 0, 0);
//        uranus.setRotationDegrees(-90f, 0, 0);
//        neptune.setRotationDegrees(-90f, 0, 0);
//        moon.setRotationDegrees(-90f, 0, 0);
//        
//        sunModel.fullBright = true;
//        mercuryModel.fullBright = true;
//        venusModel.fullBright = true;
//        marsModel.fullBright = true;
//        jupiterModel.fullBright = true;
//        saturnModel.fullBright = true;
//        uranusModel.fullBright = true;
//        neptuneModel.fullBright = true;
        
//        sun.setScale(10f * 109f);
//        mercury.setScale(10f * 0.383f);
//        venus.setScale(10f * 0.9499f);
        earth.setScale(10f);
//        moon.setScale(10f * 0.27f);
//        mars.setScale(10f * 0.53f);
//        jupiter.setScale(10f * 11.2f);
//        saturn.setScale(10f * 9.5f);
//        uranus.setScale(10f * 4.007f);
//        neptune.setScale(10f * 3.9f);
//        
//        sun.setPosition(10f * 11727.8144f, 0f, 0f);
//        mercury.setPosition(10f * 7188.4439f, 0, 0);
//        venus.setPosition(10f * 3245.5315f, 0, 0);
        earth.setPosition(0f, 0f, 0f);
//        moon.setPosition(-10f * 30f, 0f, 0f);
//        mars.setPosition(-10f * 6146.1273f, 0, 0);
//        jupiter.setPosition(-10f * 49302.2891f, 0, 0);
//        saturn.setPosition(-10f * 100533.0825f, 0, 0);
//        uranus.setPosition(-10f * 213029.1627f, 0, 0);
//        neptune.setPosition(-10f * 342223.2675f, 0, 0);
        
        addEntity(stars);
//        addEntity(sun);
//        addEntity(mercury);
//        addEntity(venus);
        addEntity(earth);
//        addEntity(moon);
//        addEntity(mars);
//        addEntity(jupiter);
//        addEntity(saturn);
//        addEntity(uranus);
//        addEntity(neptune);

//        addEntity(axis);
//        addEntity(mountains);
//        addEntity(teapot);
//        addEntity(cube1);
//        addEntity(cube2);
        //addEntity(line1);
        
        Line3D axisXLine = new Line3D(Color.RED);
        axisXLine.addPoint(-100f, 0, 0);
        axisXLine.addPoint(100f, 0, 0);
        Line3D axisYLine = new Line3D(Color.GREEN);
        axisYLine.addPoint(0, -100f, 0);
        axisYLine.addPoint(0, 100f, 0);
        Line3D axisZLine = new Line3D(Color.BLUE);
        axisZLine.addPoint(0, 0, -100f);
        axisZLine.addPoint(0, 0, 100f);
        
        Entity axisX = new Entity(axisXLine);
        Entity axisY = new Entity(axisYLine);
        Entity axisZ = new Entity(axisZLine);
        //addEntity(axisX);
        addEntity(axisY);
        //addEntity(axisZ);
        
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
