package controller.globe;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import eutil.datatypes.util.EList;
import eutil.file.EFileUtil;
import eutil.file.LineReader;
import eutil.math.ENumUtil;
import eutil.math.vectors.Vec3f;
import eutil.swing.LeftClick;

public class Test3DWindow extends JFrame implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    
    //========
    // Fields
    //========
    
    private JPanel drawPanel = new JPanel() {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g;
            
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            createWindowImage();
            updateZBuffer();
            zoomTriangles();
            setupMaths();
            
            g2.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, imgWidth, imgHeight, null);
            g2.setColor(Color.WHITE);
            String pos = "POS: " + camPos;
            var posGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), pos);
            g2.drawString(pos, 0, (int) posGV.getVisualBounds().getHeight());
            String rot = "ROT: " + camRot;
            var rotGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), rot);
            g2.drawString(rot, 0, (int) rotGV.getVisualBounds().getHeight() * 2);
            
            int midX = getWidth() / 2;
            int midY = getHeight() / 2;
            g2.setColor(Color.RED);
            g2.fillRect(midX - 4, midY, 10, 2);
            g2.fillRect(midX, midY - 4, 2, 10);
        };
    };
    
    double[] zBuffer;
    EList<Shape> shapes = EList.newList();
    Matrix3 headingTransform;
    Matrix3 pitchTransform;
    Matrix3 transform;
    BufferedImage img;
    BufferedImage world;
    
    public Vector camPos = new Vector();
    public Vector camRot = new Vector();
    public Vector lookDir = new Vector();
    
    private JButton rebuild;
    
    private double heading;
    private double pitch;
    private double zoom = 100;
    private double oldX, oldY;
    private float fov = 90f;
    private boolean lockCursor = false;
    private boolean isMovingMouse = false;
    private Robot mouseMover;
    private Cursor standard;
    private Cursor hidden;
    
    private int imgWidth = 320;
    private int imgHeight = 240;
    
    //==============
    // Constructors
    //==============
    
    public Test3DWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        setSize(500, 500);
        
        try {
            world = ImageIO.read(getClass().getResource("/world.topo.bathy.200408x294x196.jpg"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        // cursor stuff
        standard = getCursor();
        BufferedImage hiddenCursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        hidden = Toolkit.getDefaultToolkit().createCustomCursor(hiddenCursor, new Point(0, 0), "hidden");
        
        rebuild = new JButton("Rebuild");
        LeftClick.applyOn(rebuild, this::setup);
        
        add(rebuild, BorderLayout.NORTH);
        add(drawPanel, BorderLayout.CENTER);
        
        drawPanel.addMouseWheelListener(this);
        drawPanel.addMouseMotionListener(this);
        drawPanel.addMouseListener(this);
        drawPanel.addKeyListener(this);
        addKeyListener(this);
        
        rebuild.addKeyListener(this);
        
        try {
            mouseMover = new Robot();
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
        
        setup();
        setVisible(true);
        
        drawPanel.requestFocusInWindow();
    }

    //===========
    // Overrides
    //===========
    
    @Override
    public void keyPressed(KeyEvent e) {
        float amount = 1f;
        float rotAmount = 2f;
        //camRot.y = 270.0f;
        float x = (float) Math.sin(Math.toRadians(camRot.y)) * amount;
        float z = (float) Math.cos(Math.toRadians(camRot.y)) * amount;
        //System.out.println(camRot.y + " : " + (Math.sin(Math.toRadians(camRot.y))) + " | " + x + " : " + z);
        //System.out.println(camPos + " : " + camRot + " : " + x + " : " + z);
        
        if (e.getKeyCode() == KeyEvent.VK_W) camPos = camPos.add(new Vector(-x, 0, z)); //camPos.z += amount;
        if (e.getKeyCode() == KeyEvent.VK_S) camPos = camPos.add(new Vector(x, 0, -z)); //camPos.z -= amount;
        if (e.getKeyCode() == KeyEvent.VK_A) camPos = camPos.add(new Vector(z, 0, x)); //camPos.x += amount;
        if (e.getKeyCode() == KeyEvent.VK_D) camPos = camPos.add(new Vector(-z, 0, -x)); //camPos.x -= amount;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) camPos.y += amount;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) camPos.y -= amount;
        if (e.getKeyCode() == KeyEvent.VK_UP) camRot.x += rotAmount;
        if (e.getKeyCode() == KeyEvent.VK_DOWN) camRot.x -= rotAmount;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) camRot.y += rotAmount;
        if (e.getKeyCode() == KeyEvent.VK_LEFT) camRot.y -= rotAmount;
        
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_T) {
            if (lockCursor) lockCursor(false);
        }
        
        //System.out.println(camRot);
        //System.out.println(x + " : " + z + " : " + camPos);
        repaint();
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!lockCursor || isMovingMouse) return;
        
        float dx = e.getX() - drawPanel.getWidth() / 2;
        float dy = e.getY() - drawPanel.getHeight() / 2;
//        double x = e.getX();
//        double y = e.getY();
//        
//        if (Double.isNaN(oldX)) oldX = x;
//        if (Double.isNaN(oldY)) oldY = y;
//        
//        float dx = (float) (x - oldX);
//        float dy = (float) (y - oldY);
//        
//        oldX = x;
//        oldY = y;
        
        isMovingMouse = true;
        var loc = drawPanel.getLocationOnScreen();
        mouseMover.mouseMove(loc.x + drawPanel.getWidth() / 2,
                             loc.y + drawPanel.getHeight() / 2);
        isMovingMouse = false;
        
        dx /= 180.0;
        dy /= -180.0;
        
        heading += dx;
        pitch += dy;
        pitch = ENumUtil.clamp(pitch, -Math.PI / 2, Math.PI / 2);
        
        float fovRatio = fov / 110f;
        dx *= fovRatio;
        dy *= fovRatio;
        
        camRot = camRot.add(new Vector(dy, dx, 0).mul(15f));
        camRot = new Vector(ENumUtil.clamp(camRot.x, -90, 90), camRot.y, 0);
        
        drawPanel.repaint();
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {

    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double amount = (e.getScrollAmount()) * e.getWheelRotation() * -5.0;
        zoom += amount;
        
        fov += e.getWheelRotation() * 3f;
        fov = ENumUtil.clamp(fov, 10f, 110f);
        
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) {
            lockCursor(true);
        }
        if (e.getButton() == 2) {
            heading = 0;
            pitch = Math.PI / 2;
            zoom = 0;
            camPos.set(0.0f, 0.0f, 0.0f);
            camRot.set(0.0f, 0.0f, 0.0f);
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        oldX = Double.NaN;
        oldY = Double.NaN;
    }
    
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    
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
    
    //=========
    // Methods
    //=========
    
    public void setup() {
        shapes.clear();
        
        //imgWidth = 320; imgHeight = 240;
        imgWidth = 640; imgHeight = 480;
        //imgWidth = 1080; imgHeight = 720;
        //imgWidth = 1920; imgHeight = 1080;
        //camPos.z = -2.0f;
        
        Sphere sphere1 = new Sphere(10, 20, 10, 3.0f);
        sphere1.setRotation(90.0f, 0.0f, 0.0f);
        //Sphere sphere2 = new Sphere(100.0f);
        
        //Cube c1 = new Cube(0, 1.5f, 0, 0.5f, 0.5f, 0.5f);
        //Cube c2 = new Cube(0.9f, 1.5f, 0, 0.5f, 0.5f, 0.5f);
        
       
        //triangles.addAll(sphere2.triangles);
        //triangles.addAll(c1.triangles);
        //triangles.addAll(c2.triangles);
        
//        var s1 = new Triangle(new Vector(0.0f, 0.0f, 0.0f), new Vector(0.0f, 1.0f, 0.0f), new Vector(1.0f, 1.0f, 0.0f), Color.WHITE);
//        var s2 = new Triangle(new Vector(0.0f, 0.0f, 0.0f), new Vector(1.0f, 1.0f, 0.0f), new Vector(1.0f, 0.0f, 0.0f), Color.WHITE);
//        
//        var e1 = new Triangle(new Vector(1.0f, 0.0f, 0.0f), new Vector(1.0f, 1.0f, 0.0f), new Vector(1.0f, 1.0f, 1.0f), Color.RED);
//        var e2 = new Triangle(new Vector(1.0f, 0.0f, 0.0f), new Vector(1.0f, 1.0f, 0.0f), new Vector(1.0f, 0.0f, 1.0f), Color.RED);
//        
//        var n1 = new Triangle(new Vector(1.0f, 0.0f, 1.0f), new Vector(1.0f, 1.0f, 1.0f), new Vector(0.0f, 1.0f, 1.0f), Color.GREEN);
//        var n2 = new Triangle(new Vector(1.0f, 0.0f, 1.0f), new Vector(0.0f, 1.0f, 1.0f), new Vector(0.0f, 0.0f, 1.0f), Color.GREEN);
//        
//        var w1 = new Triangle(new Vector(0.0f, 0.0f, 1.0f), new Vector(0.0f, 1.0f, 1.0f), new Vector(0.0f, 1.0f, 0.0f), Color.BLUE);
//        var w2 = new Triangle(new Vector(0.0f, 0.0f, 1.0f), new Vector(0.0f, 1.0f, 0.0f), new Vector(0.0f, 0.0f, 0.0f), Color.BLUE);
//        
//        var t1 = new Triangle(new Vector(0.0f, 1.0f, 0.0f), new Vector(0.0f, 1.0f, 1.0f), new Vector(1.0f, 1.0f, 1.0f), Color.MAGENTA);
//        var t2 = new Triangle(new Vector(0.0f, 1.0f, 0.0f), new Vector(1.0f, 1.0f, 1.0f), new Vector(1.0f, 1.0f, 0.0f), Color.MAGENTA);
//        
//        var b1 = new Triangle(new Vector(1.0f, 0.0f, 1.0f), new Vector(0.0f, 0.0f, 1.0f), new Vector(0.0f, 0.0f, 0.0f), Color.YELLOW);
//        var b2 = new Triangle(new Vector(1.0f, 0.0f, 1.0f), new Vector(0.0f, 0.0f, 0.0f), new Vector(1.0f, 0.0f, 0.0f), Color.YELLOW);
//        
//        triangles.add(s1, s2, e1, e2, n1, n2, w1, w2, t1, t2, b1, b2);
        
        //load("axis.obj");
        Shape mountains = load("mountains.obj");
        mountains.position.set(0.0f, -50f, 0.0f);
        
        //Shape doom1 = load("doom_E1M1.obj");
        //addShape(doom1);
        
        addShape(sphere1);
        addShape(mountains);
        
//        for (int i = 0; i < 3; i++) {
//            triangles = inflate(triangles);
//        }
        
        this.repaint();
    }
    
    public void addShape(Shape shape) {
        if (shape == null) return;
        shapes.add(shape);
    }
    
    public Shape load(String fileName) {
        try {
            var loader = Thread.currentThread().getContextClassLoader();
            File file = new File(loader.getResource(fileName).toURI());
            
            if (!EFileUtil.fileExists(file)) return null;
            
            try (var r = new LineReader(file)) {
                EList<Triangle> loaded = EList.newList();
                EList<Vector> verts = EList.newList();
                
                while (r.hasNextLine()) {
                    String line = r.nextLine();
                    String[] parts = line.split(" ");
                    
                    // comments
                    if (parts.length == 0 || parts[0].equals("#")) continue;
                    
                    if (parts.length == 4) {
                        // vertex
                        if (parts[0].equals("v")) {
                            float x = Float.parseFloat(parts[1]);
                            float y = Float.parseFloat(parts[2]);
                            float z = Float.parseFloat(parts[3]);
                            verts.add(new Vector(x, y, z));
                        }
                        if (parts[0].equals("f")) {
                            int f0 = Integer.parseInt(parts[1]);
                            int f1 = Integer.parseInt(parts[2]);
                            int f2 = Integer.parseInt(parts[3]);
                            Vector v0 = verts.get(f0 - 1);
                            Vector v1 = verts.get(f1 - 1);
                            Vector v2 = verts.get(f2 - 1);
                            loaded.add(new Triangle(v0, v1, v2, Color.WHITE));
                        }
                    }
                }
                
                Shape model = new Shape() {};
                model.triangles.addAll(loaded);
                return model;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void setupMaths() {
        float width = imgWidth;
        float height = imgHeight;
        
//        float angleX = (float) Math.toRadians(90.0f);
//        float angleY = (float) Math.toRadians(0.0f);
//        float angleZ = (float) Math.toRadians(0.0f);
        
        camRot = new Vector(ENumUtil.clamp(camRot.x, -90, 90), camRot.y, 0);
        camRot = new Vector(camRot.x, camRot.y % 360.0f, 0);
        if (camRot.y < 0) camRot.y += 360.0f;
        
//        Matrix4 rotX = Matrix4.makeRotationX(angleX);
//        Matrix4 rotY = Matrix4.makeRotationY(angleY);
//        Matrix4 rotZ = Matrix4.makeRotationZ(angleZ);
        
        Matrix4 projection = Matrix4.makeProjection(fov, height / width, 0.1f, 1000.0f);
//        Matrix4 translated = Matrix4.makeTranslation(0.0f, 0.0f, 5.0f);
        
//        Matrix4 world = new Matrix4();
//        world = world.rotateXYZ(angleX, angleY, angleZ);
//        world = world.multiply(translated);
        
        // create look at camera matrix
//        Vector up = new Vector(0, 1, 0);
//        Vector target = new Vector(0, 0, 1);=
//        Matrix4 cameraRot = new Matrix4();
//        cameraRot = cameraRot.multiply(Matrix4.makeRotationX(camRot.x));
//        cameraRot = cameraRot.multiply(Matrix4.makeRotationY(camRot.y));
//        cameraRot = cameraRot.multiply(Matrix4.makeRotationZ(camRot.z));
//        lookDir = cameraRot.multiply(target);
//        target = camPos.add(lookDir);
//        Matrix4 camera = new Matrix4().pointAt(camPos, target, up);
        
        Matrix4f c2 = new Matrix4f();
        Vector3f eye = new Vector3f(camPos.x, camPos.y, camPos.z);
        Vector3f cameraFront = new Vector3f(0f, 0f, 1f);
        Vector3f cameraUp = new Vector3f(0f, 1f, 0f);
        cameraFront.add(camPos.x, camPos.y, 0.0f);
        c2.identity();
        c2.rotateX((float) Math.toRadians(camRot.x));
        c2.rotateY((float) Math.toRadians(camRot.y));
        c2.rotateZ((float) Math.toRadians(camRot.z));
        c2.lookAt(eye, cameraFront, cameraUp);
        
        // create view matrix
        //Matrix4 view = Matrix4.quickInverse(camera);
        Matrix4 view = new Matrix4();
        view.set(c2.m00(), c2.m01(), c2.m02(), c2.m03(),
                 c2.m10(), c2.m11(), c2.m12(), c2.m13(),
                 c2.m20(), c2.m21(), c2.m22(), c2.m23(),
                 c2.m30(), c2.m31(), c2.m32(), c2.m33());
        
        Vector NEAR_PLANE = new Vector(0.0f, 0.0f, 0.1f);
        Vector FAR_PLANE = new Vector(0.0f, 0.0f, 1.0f);
        Vector offsetView = new Vector(1, 1, 0);
        
//        System.out.println("WORLD:\n" + world);
        //System.out.println("VIEW:\n" + view);
        
        EList<Triangle> toDraw = EList.newList();
        for (var shape : shapes) {
            for (var tri : shape.triangles) {
                // world transform
                Matrix4 worldTransform = Matrix4.makeTransform(shape);
                Triangle transformed = worldTransform.multiply(tri);
                
                // calculate normal
                Vector line1 = transformed.v1.sub(transformed.v0);
                Vector line2 = transformed.v2.sub(transformed.v0);
                Vector normal = line1.cross(line2).norm();
                
                // get ray from triangle to camera
                Vector cameraRay = transformed.v0.sub(camPos);
                
                // only render triangles that are actually visible from the view of the camera
                if (normal.dot(cameraRay) < 0.0f) {
                    // lighting
                    Vector light_direction = new Vector(0.0f, 0.3f, -1.0f).norm();
                    float dp = Math.max(0.1f, light_direction.dot(normal));
                    transformed.color = getShade(transformed.color, dp);
                    
                    // transform from world space into view space
                    Triangle triView = new Triangle(transformed);
                    triView.v0 = view.multiply(triView.v0);
                    triView.v1 = view.multiply(triView.v1);
                    triView.v2 = view.multiply(triView.v2);
                    
                    // clip view triangle against near plane, this could form two additional triangles
                    Triangle[] clipped = triView.clipAgainstPlane(NEAR_PLANE, FAR_PLANE);
                    for (int i = 0; i < clipped.length; i++) {
                        Triangle projected = new Triangle(clipped[i]);
                        //Triangle projected = new Triangle(triView);
                        
                        // project from 3D to 2D
                        projected.v0 = projection.multiply(projected.v0);
                        projected.v1 = projection.multiply(projected.v1);
                        projected.v2 = projection.multiply(projected.v2);
                        
                        // scale into view
                        projected.v0 = projected.v0.div(projected.v0.w);
                        projected.v1 = projected.v1.div(projected.v1.w);
                        projected.v2 = projected.v2.div(projected.v2.w);
                        // x/y are inverted so put them back
                        projected.v0.x *= -1.0f;
                        projected.v1.x *= -1.0f;
                        projected.v2.x *= -1.0f;
                        projected.v0.y *= -1.0f;
                        projected.v1.y *= -1.0f;
                        projected.v2.y *= -1.0f;
                        // offset verts into visible normalized space
                        projected.v0 = projected.v0.add(offsetView);
                        projected.v1 = projected.v1.add(offsetView);
                        projected.v2 = projected.v2.add(offsetView);
                        projected.v0.x *= 0.5f * width; projected.v0.y *= 0.5f * height;
                        projected.v1.x *= 0.5f * width; projected.v1.y *= 0.5f * height;
                        projected.v2.x *= 0.5f * width; projected.v2.y *= 0.5f * height;
                        
                        // add to draw list
                        toDraw.add(projected);
                    }
                }
            }
        }
        
        // sort draw list
        toDraw.sort((t1, t2) -> {
            float z1 = (t1.v0.z + t1.v1.z + t1.v2.z) / 3.0f;
            float z2 = (t2.v0.z + t2.v1.z + t2.v2.z) / 3.0f;
            return Float.compare(z1, z2);
        });
        
        toDraw = toDraw.reverse();
        
        for (Triangle tri : toDraw) {
            // clip triangles against all four screen edges, this could yield
            // a bunch of triangles, so create a queue that we traverse to
            // ensure we only test new triangles generated against planes
            EList<Triangle> listTriangles = EList.of(tri);
            int newTriangles = 1;
            
            for (int p = 0; p < 4; p++) {
                while (newTriangles > 0) {
                    // take triangle from front of queue
                    Triangle[] clipped = null;
                    Triangle test = listTriangles.pop();
                    newTriangles--;
                    
                    // clip it against a plane
                    switch (p) {
                    case 0: clipped = test.clipAgainstPlane(new Vector(0.0f, 0.0f, 0.0f), new Vector(0.0f, 1.0f, 0.0f)); break;
                    case 1: clipped = test.clipAgainstPlane(new Vector(0.0f, height - 1, 0.0f), new Vector(0.0f, -1.0f, 0.0f)); break;
                    case 2: clipped = test.clipAgainstPlane(new Vector(0.0f, 0.0f, 0.0f), new Vector(1.0f, 0.0f, 0.0f)); break;
                    case 3: clipped = test.clipAgainstPlane(new Vector(width - 1, 0.0f, 0.0f), new Vector(-1.0f, 0.0f, 0.0f)); break;
                    }
                    
                    // clipping may yield a variable number of triangles, so add these new ones
                    // to the back of the queue for subsequent clipping against next planes
                    if (clipped == null) continue;
                    for (int w = 0; w < clipped.length; w++) {
                        listTriangles.add(clipped[w]);
                    }
                }
                newTriangles = listTriangles.size();
            }
            
            for (Triangle tt : listTriangles) {
                Vector v0 = tt.v0;
                Vector v1 = tt.v1;
                Vector v2 = tt.v2;
                
                int minX = (int) Math.max(0, Math.ceil(Math.min(v0.x, Math.min(v1.x, v2.x))));
                int maxX = (int) Math.min(imgWidth - 1, Math.floor(Math.max(v0.x, Math.max(v1.x, v2.x))));
                int minY = (int) Math.max(0, Math.ceil(Math.min(v0.y, Math.min(v1.y, v2.y))));
                int maxY = (int) Math.min(imgHeight - 1, Math.floor(Math.max(v0.y, Math.max(v1.y, v2.y))));
                
                double triangleArea = (v0.y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - v0.x);
                
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        double b1 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                        double b2 = ((y - v0.y) * (v2.x - v0.x) + (v2.y - v0.y) * (v0.x - x)) / triangleArea;
                        double b3 = ((y - v1.y) * (v0.x - v1.x) + (v0.y - v1.y) * (v1.x - x)) / triangleArea;
                        
                        if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                            double depth = b1 * v0.z + b2 * v1.z + b3 * v2.z;
                            int zIndex = y * imgWidth + x;
                            
                            if (zBuffer[zIndex] < depth) {
                                img.setRGB(x, y, tt.color.getRGB());
                                zBuffer[zIndex] = depth;
                            }
                        }
                    }
                }
            }
        }
    }
    
    private float dist(Vec3f n, Vec3f v, float dot) {
        return (n.x * v.x + n.y * v.y + n.z * v.z - dot);
    }
    
    private void createWindowImage() {
        img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        zBuffer = new double[imgWidth * imgHeight];
    }
    
    private void updateZBuffer() {
        for (int i = 0; i < zBuffer.length; i++) {
            zBuffer[i] = Double.NEGATIVE_INFINITY;
        }
    }
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    public EList<Triangle> inflate(EList<Triangle> tris) {
        EList<Triangle> result = EList.newList();
//        for (Triangle t : tris) {
//            Vector m1 = new Vector((t.v1.x + t.v2.x)/2, (t.v1.y + t.v2.y)/2, (t.v1.z + t.v2.z)/2);
//            Vector m2 = new Vector((t.v2.x + t.v3.x)/2, (t.v2.y + t.v3.y)/2, (t.v2.z + t.v3.z)/2);
//            Vector m3 = new Vector((t.v1.x + t.v3.x)/2, (t.v1.y + t.v3.y)/2, (t.v1.z + t.v3.z)/2);
//            
//            result.add(new Triangle(t.v1, m1, m3, t.color));
//            result.add(new Triangle(t.v2, m1, m2, t.color));
//            result.add(new Triangle(t.v3, m2, m3, t.color));
//            result.add(new Triangle(m1  , m2, m3, t.color));
//        }
        return result;
    }
    
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.0) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.0) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.0) * shade;

        int red = (int) Math.pow(redLinear, 1 / 2.0);
        int green = (int) Math.pow(greenLinear, 1 / 2.0);
        int blue = (int) Math.pow(blueLinear, 1 / 2.0);

        return new Color(red, green, blue);
    }
    
    public void zoomTriangles() {
//        for (Triangle t : triangles) {
//            for (Vector v : new Vector[] { t.v1, t.v2, t.v3 }) {
//                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / zoom;
//                v.x /= l;
//                v.y /= l;
//                v.z /= l;
//            }
//        }
    }

}