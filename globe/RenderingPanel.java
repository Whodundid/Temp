package controller.globe;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import controller.globe.input.KeyInput;
import controller.globe.input.MouseLookController;
import controller.globe.math.Vertex;
import controller.globe.models.Cube;
import controller.globe.models.Line3D;
import controller.globe.models.Model;
import controller.globe.models.Sphere;
import controller.globe.models.Triangle;
import controller.globe.util.ObjLoader;
import controller.globe.util.ObjZipLoader;
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
    
    private int renderScale = 50;
    private int imgWidth = 320;
    private int imgHeight = 240;
    
    private int lastX, lastY;
    private volatile boolean leftPress;
    
    Entity earth;
    Entity cube1, cube2;
    Entity t1;
    Entity line1;
    Entity teapot;
    
    private long tps = 60;
    private double timeT = 1000.0 / tps;
    private double deltaT = 0;
    private int curNumTicks = 0;
    private int ticks = 0;
    
    private long fps = 240;
    private double timeF = 1000.0 / fps;
    private double deltaF = 0;
    private long startTime = 0L;
    private long curTime = 0L;
    private long oldTime = 0L;
    private float dt = 0.0f;
    private long timer;
    private long initialTime = 0L;
    private long runningTime = 0L;
    private int frames = 0;
    private int curFrameRate = 0;
    
    private MouseLookController mouseLook;
    private KeyInput keyInput;
    
    //==============
    // Constructors
    //==============
    
    public RenderingPanel(int width, int height) {
        imgWidth = width;
        imgHeight = height;
        
        camera = new Camera();
        camera.position.set(0, 0, 30);
        renderer = new PerspectiveRenderer(width, height);
        
        mouseLook = new MouseLookController(this, camera);
        this.addMouseListener(mouseLook);
        this.addMouseMotionListener(mouseLook);
        this.addKeyListener(mouseLook);
        this.addMouseWheelListener(mouseLook);
        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { mouseLook.onSurfaceChanged(); }
            @Override public void componentMoved(ComponentEvent e) { mouseLook.onSurfaceChanged(); }
        });
        
        keyInput = new KeyInput();
        keyInput.attachTo(this);
        
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        
        // prepare timers
        startTime = System.nanoTime();
        //oldTime = startTime;
        //timer = startTime;
        initialTime = System.nanoTime();
        
        new Thread(() -> {
            while (true) {
                thing();
                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException ignored) {}
            }
        }).start();
    }
    
    private void thing() {
        try {
            long now = System.nanoTime();
            double elapsedMs = (now - initialTime) / 1_000_000.0;
            initialTime = now;

            deltaT += elapsedMs / timeT;
            deltaF += elapsedMs / timeF;

            if (deltaT >= 1) {
                oldTime = curTime;
                curTime = System.currentTimeMillis();
                dt = curTime - oldTime;
                ticks++;
                runTick(dt);
                deltaT--;
            }

            if (deltaF >= 1) {
                runRenderTick(curTime - oldTime);
            }

            long nowMillis = System.currentTimeMillis();
            if (nowMillis - timer > 1000) {
                curFrameRate = frames;
                curNumTicks = ticks;
                frames = 0;
                ticks = 0;
                timer = nowMillis;
            }

            if (deltaT > 3 || deltaF > 5) {
                deltaT = 0;
                deltaF = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
    private void runTick(float dt) {
        keyInput.tick();
        
        // input
        if (keyInput.isDown(KeyEvent.VK_W)) camera.onKeyPressed(KeyEvent.VK_W);
        if (keyInput.isDown(KeyEvent.VK_S)) camera.onKeyPressed(KeyEvent.VK_S);
        if (keyInput.isDown(KeyEvent.VK_A)) camera.onKeyPressed(KeyEvent.VK_A);
        if (keyInput.isDown(KeyEvent.VK_D)) camera.onKeyPressed(KeyEvent.VK_D);

        // World-absolute up/down
        if (keyInput.isDown(KeyEvent.VK_SPACE)) camera.onKeyPressed(KeyEvent.VK_SPACE);
        if (keyInput.isDown(KeyEvent.VK_SHIFT)) camera.onKeyPressed(KeyEvent.VK_SHIFT);

        // Edge-trigger actions (fire once)
        if (keyInput.isDown(KeyEvent.VK_Q)) camera.onKeyPressed(KeyEvent.VK_Q);
        if (keyInput.isDown(KeyEvent.VK_E)) camera.onKeyPressed(KeyEvent.VK_E);
        
        if (t1 != null) t1.rotation.y += 0.001f;
        if (earth != null && !leftPress) earth.rotation.z += 0.0001f;
        if (line1 != null && !leftPress) line1.rotation.y += 0.0001f;
        if (teapot != null) {
//            teapot.position.x -= 0.01f;
            teapot.rotation.y += 0.11f;
//            teapot.scale.set(1.0f, 1.0f, 1.0f);
            teapot.scale.set(teapot.scale.mul(0.9999f));
        }
        if (cube1 != null) {
            cube1.rotation.x += 0.01f;
            cube1.rotation.y += 0.001f;
            cube1.rotation.z += 0.05f;
        }
        if (cube2 != null) cube2.rotation.z -= 0.01f;
        
        synchronized (entities) {
            for (var e : entities) {
                e.update(dt);
            }
        }
        
        renderer.setLightDegrees(renderer.getLightDegrees() + 1f);
    }
    
    private void runRenderTick(long dt) {
        repaint();
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
        boolean blend = true;
        boolean useSSAA = true;
        renderer.render(camera, entities, img, getWidth(), getHeight(), blend, useSSAA);
        
        // draw debug camera position and rotation
        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        
        g2.setColor(Color.WHITE);
        String pos = "POS: " + camera.position;
        var posGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), pos);
        g2.drawString(pos, 0, (int) posGV.getVisualBounds().getHeight());
        String rot = "ROT: " + camera.getOrientation();
        var rotGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), rot);
        g2.drawString(rot, 0, (int) rotGV.getVisualBounds().getHeight() * 2);
        String fov = "FOV: " + renderer.getFOV();
        var fovGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), fov);
        g2.drawString(fov, 0, (int) fovGV.getVisualBounds().getHeight() * 4 + 2);
        String quality = "Q: " + renderScale;
        var qGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), quality);
        g2.drawString(quality, 0, (int) qGV.getVisualBounds().getHeight() * 5 + 1);
        String fps = "FPS: " + curFrameRate;
        var fpsGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), fps);
        g2.drawString(fps, 0.0f, (float) fpsGV.getVisualBounds().getHeight() * 6.5f + 0.5f);
        String tps = "TPS: " + curNumTicks;
        var tpsGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), tps);
        g2.drawString(tps, 0.0f, (float) tpsGV.getVisualBounds().getHeight() * 8f - 0.5f);
        
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
        
        frames++;
        deltaF--;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
//        camera.onKeyPressed(e);
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
//        if (!lockCursor) return;
//        
//        float dx = e.getX() - getWidth() / 2;
//        float dy = e.getY() - getHeight() / 2;
//        
//        var loc = getLocationOnScreen();
//        mouseMover.mouseMove(loc.x + getWidth() / 2,
//                             loc.y + getHeight() / 2);
//        
//        dx /= 180.0;
//        dy /= -180.0;
//        
//        float fovRatio = renderer.getFOV() / 120f;
//        dx *= fovRatio;
//        dy *= fovRatio;
//        
//        camera.updateLook(-dy * 15f, -dx * 15f, 0);
//        repaint();
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
        if (e.getButton() == 2) {
            camera.resetOrientation();
            camera.position.set(0, 0, 30);
            repaint();
        }
        if (e.getButton() == 3) {
            lastX = e.getX();
            lastY = e.getY();
            leftPress = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == 3) leftPress = false;
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (leftPress) {
            int x = e.getX();
            int y = e.getY();
            
            float dx = x - lastX;
            float dy = y - lastY;
            
            float amount = 0.003f;
            dx *= amount;
            dy *= amount;
            
            earth.rotation.addT(0, 0, -dx);
            line1.rotation.addT(0, -dx, 0);
            
            lastX = x;
            lastY = y;
        }
    }
    
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}    
    //=========
    // Methods
    //=========
    
    @SuppressWarnings("unused")
    public void setup() {
        entities.clear();
        
        imgWidth = 21 * renderScale;
        imgHeight = 9 * renderScale;
//        System.out.println(getWidth() + " : " + getHeight());
        
        //imgWidth = 256; imgHeight = 144;
        //imgWidth = 320; imgHeight = 240;
        //imgWidth = 480; imgHeight = 270; // 30
        //imgWidth = 640; imgHeight = 360; // 40
        //imgWidth = 800; imgHeight = 450; // 50
        
        //imgWidth = 1280; imgHeight = 720;
        //imgWidth = 1920; imgHeight = 1080;
        
        Sphere starsModel = new Sphere(2000.0f, 10, 10, Test3DWindow.stars);
        starsModel.insideOut = true;
        starsModel.fullBright = true;
        Entity stars = new Entity("Stars", starsModel);
        stars.setRotationDegrees(90f, 0, 0);
        stars.setPosition(0, 0, 0);
        
        //Sphere planetModel = new Sphere(1.0f, 70, 70, Test3DWindow.world);
        Sphere planetModel = new Sphere(1.0f, 70, 70, Test3DWindow.worldBig);
        planetModel.fullBright = true;
        planetModel.minimumBrightness = 0.3f;
        earth = new Entity("Planet", planetModel);
        earth.setRotationDegrees(-90.0f, 0f, 0.0f);
        earth.setScale(1.0f, 0.9966f, 1.0f);
        
        
        Model axisModel = Model.loadModelFromObjFile("axis.obj");
        Model mountainsModel = Model.loadModelFromObjFile("mountains.obj");
        Model teapotModel = Model.loadModelFromObjFile("teapot.obj");
        
        Model doomModel = ObjZipLoader.loadModelFromZip("DOOM_E1M1.zip");
        doomModel.insideOut = false;
        doomModel.fullBright = true;
        Entity doom = new Entity("Doom", doomModel);
        doom.setPosition(40, 0, 400);
        doom.setScale(0.2f);
//        addEntity(doom);
        
//        Model dragon = ObjLoader.loadModelFromObjFile("dragon.obj");
//        dragon.insideOut = false;
//        dragon.fullBright = false;
//        Entity drag = new Entity("Grag", dragon);
//        drag.setPosition(40, 0, 0);
//        drag.setScale(1f);
//        addEntity(drag);
        
//        Model dragon = ObjLoader.loadModelFromObjFile("chinaberry_2.obj");
//        dragon.insideOut = false;
//        dragon.fullBright = false;
//        Entity drag = new Entity("Grag", dragon);
//        drag.setPosition(40, 0, 0);
//        drag.setScale(0.01f);
//        addEntity(drag);
        
//        Model earthModel = ObjZipLoader.loadModelFromZip("thing.zip");
//        earthModel.fullBright = true;
//        Entity earthModelEntity = new Entity("EarthEntity", earthModel);
//        earthModelEntity.setPosition(-50, 0, 0);
//        addEntity(earthModelEntity);
        
        Entity axis = new Entity("Axis", axisModel);
        Entity mountains = new Entity("Mountains", mountainsModel);
        teapot = new Entity("Teapot", teapotModel);
        axis.setPosition(0, -10f, -25.0f);
        teapot.setPosition(30.0f, 0.0f, 0.0f);
        teapot.setScale(0.15f);
        mountains.setPosition(0.0f, -50f, 0.0f);
        
        Cube cubeModel1 = new Cube(/*Test3DWindow.world*/);
        Cube cubeModel2 = new Cube(new Color(0xbb9b9b9b, true), true);
        cubeModel1.fullBright = true;
        cubeModel2.fullBright = true;
        //cubeModel.setTexture(Test3DWindow.world);
        cube1 = new Entity("Cube1", cubeModel1);
        cube2 = new Entity("Cube2", cubeModel2);
        cube1.setPosition(20.0f, 0.0f, 20.0f);
        cube2.setPosition(20.0f, 0.0f, 15.0f);
        
        Line3D line11 = new Line3D();
        line11.addPoint(7.25f, 0.0f, 7.25f);
        line11.addPoint(10.5f, 0.5f, 11.5f);
        line11.addPoint(11.0f, 2.0f, 12.0f);
        line11.color = Color.GREEN;
        line1 = new Entity("Line1", line11);
        line11.antiAlias = true;
        line11.lineWidth = 1;
        
        Triangle tm1 = new Triangle(new Vertex(20.0f, 0.0f, 10.0f), new Vertex(20.0f, 0.0f, 5.0f), new Vertex(20.0f, 5.0f, 5.0f));
        tm1.alwaysFaceCamera = true;
        t1 = new Entity("Triangle1", tm1);
        addEntity(t1);
        
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
        
//        addEntity(stars);
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
//        addEntity(line1);
        
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
        //addEntity(axisY);
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
