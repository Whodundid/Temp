package controller.globe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import eutil.datatypes.util.EList;
import eutil.math.ENumUtil;
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
            updateTransforms();
            updateTriangles();
            
            g2.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, imgWidth, imgHeight, null);
        };
    };
    
    double[] zBuffer;
    EList<Triangle> triangles = EList.newList();
    Matrix3 headingTransform;
    Matrix3 pitchTransform;
    Matrix3 transform;
    BufferedImage img;
    
    BufferedImage world;
    
    private JButton rebuild;
    
    private double heading;
    private double pitch;
    private double zoom = 100;
    
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
        
//        headingSlider = new JSlider(-180, 180, 0);
//        pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        
//        headingSlider.addChangeListener(e -> drawPanel.repaint());
//        pitchSlider.addChangeListener(e -> drawPanel.repaint());
        
//        add(headingSlider, BorderLayout.SOUTH);
//        add(pitchSlider, BorderLayout.WEST);
        
        rebuild = new JButton("Rebuild");
        LeftClick.applyOn(rebuild, this::setup);
        
        add(rebuild, BorderLayout.NORTH);
        add(drawPanel, BorderLayout.CENTER);
        
        drawPanel.addMouseWheelListener(this);
        drawPanel.addMouseMotionListener(this);
        drawPanel.addMouseListener(this);
        drawPanel.addKeyListener(this);
        
        setup();
        setVisible(true);
    }

    //=========
    // Methods
    //=========
    
    public void setup() {
        triangles.clear();
        
        imgWidth = 1080;
        imgHeight = 720;
        
        Sphere sphere = new Sphere();
        
        Cube c1 = new Cube(150, 150, 0, 50, 50, 50);
        Cube c2 = new Cube(90, 150, 0, 50, 50, 50);
        
        triangles.addAll(sphere.triangles);
        triangles.addAll(c1.triangles);
        triangles.addAll(c2.triangles);
        
//        triangles.add(new Triangle(new Vertex(100, 100, 100),
//                              new Vertex(-100, -100, 100),
//                              new Vertex(-100, 100, -100),
//                              Color.WHITE));
//        triangles.add(new Triangle(new Vertex(100, 100, 100),
//                              new Vertex(-100, -100, 100),
//                              new Vertex(100, -100, -100),
//                              Color.RED));
//        triangles.add(new Triangle(new Vertex(-100, 100, -100),
//                              new Vertex(100, -100, -100),
//                              new Vertex(100, 100, 100),
//                              Color.GREEN));
//        triangles.add(new Triangle(new Vertex(-100, 100, -100),
//                              new Vertex(100, -100, -100),
//                              new Vertex(-100, -100, 100),
//                              Color.BLUE));
//        
//        for (int i = 0; i < 3; i++) {
//            triangles = inflate(triangles);
//        }
        
        this.repaint();
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
    
    private void updateTransforms() {
        Matrix3 headingTransform = new Matrix3(new double[] {
            Math.cos(heading), 0.0, -Math.sin(heading),
            0.0              , 1.0, 0.0,
            Math.sin(heading), 0.0, Math.cos(heading)
        });
        
        pitchTransform = new Matrix3(new double[] {
            1.0,  0.0              , 0.0,
            0.0,  Math.cos(pitch), Math.sin(pitch),
            0.0, -Math.sin(pitch), Math.cos(pitch)
        });
        
        transform = headingTransform.multiply(pitchTransform);
    }
    
    private void updateTriangles() {
        final double width = imgWidth;
        final double height = imgHeight;
        
        for (Triangle t : triangles) {
            Vertex v1 = transform.transform(t.v1);
            v1.x += width / 2;
            v1.y += height / 2;
            Vertex v2 = transform.transform(t.v2);
            v2.x += width / 2;
            v2.y += height / 2;
            Vertex v3 = transform.transform(t.v3);
            v3.x += width / 2;
            v3.y += height / 2;
            
            Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
            Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
            
            Vertex norm = new Vertex(
                ab.y * ac.z - ab.z * ac.y,
                ab.z * ac.x - ab.x * ac.z,
                ab.x * ac.y - ab.y * ac.x
            );
            
            double normLen = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
            norm.x /= normLen;
            norm.y /= normLen;
            norm.z /= normLen;
            
            double angleCos = Math.abs(norm.z);
            
            int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
            int maxX = (int) Math.min(imgWidth - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
            int maxY = (int) Math.min(imgHeight - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));
            
            double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
            
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                    double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                    double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                    
                    if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                        double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                        int zIndex = y * imgWidth + x;
                        
                        if (zBuffer[zIndex] < depth) {
                            img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
                            //img.setRGB(x, y, world.getRGB(x % world.getWidth(), y % world.getHeight()));
                            zBuffer[zIndex] = depth;
                        }
                    }
                }
            }
        }
    }
    
    public double width() { return drawPanel.getWidth(); }
    public double height() { return drawPanel.getHeight(); }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom += (e.getScrollAmount()) * e.getWheelRotation() * -5.0;
        
        repaint();
    }

    private double oldX, oldY;
    
    @Override
    public void mouseDragged(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        
        if (Double.isNaN(oldX)) oldX = x;
        if (Double.isNaN(oldY)) oldY = y;
        
        double dx = x - oldX;
        double dy = y - oldY;
        
        oldX = x;
        oldY = y;
        
        dx /= 180.0;
        dy /= -180.0;
        
        heading += dx;
        pitch += dy;
        pitch = ENumUtil.clamp(pitch, -Math.PI / 2, Math.PI / 2);
        
        drawPanel.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == 2) {
            heading = 0;
            pitch = Math.PI / 2;
            zoom = 0;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        oldX = Double.NaN;
        oldY = Double.NaN;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    public EList<Triangle> inflate(EList<Triangle> tris) {
        EList<Triangle> result = EList.newList();
        for (Triangle t : tris) {
            Vertex m1 = new Vertex((t.v1.x + t.v2.x)/2, (t.v1.y + t.v2.y)/2, (t.v1.z + t.v2.z)/2);
            Vertex m2 = new Vertex((t.v2.x + t.v3.x)/2, (t.v2.y + t.v3.y)/2, (t.v2.z + t.v3.z)/2);
            Vertex m3 = new Vertex((t.v1.x + t.v3.x)/2, (t.v1.y + t.v3.y)/2, (t.v1.z + t.v3.z)/2);
            
            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(t.v2, m1, m2, t.color));
            result.add(new Triangle(t.v3, m2, m3, t.color));
            result.add(new Triangle(m1  , m2, m3, t.color));
        }
        return result;
    }
    
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1 / 2.4);
        int green = (int) Math.pow(greenLinear, 1 / 2.4);
        int blue = (int) Math.pow(blueLinear, 1 / 2.4);

        return new Color(red, green, blue);
    }
    
    public void zoomTriangles() {
//        for (Triangle t : triangles) {
//            for (Vertex v : new Vertex[] { t.v1, t.v2, t.v3 }) {
//                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / zoom;
//                v.x /= l;
//                v.y /= l;
//                v.z /= l;
//            }
//        }
    }

}
