package controller.globe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;

import eutil.datatypes.util.EList;
import eutil.file.EFileUtil;
import eutil.file.LineReader;
import eutil.swing.LeftClick;

public class Test3DWindow extends JFrame {
    
    //========
    // Fields
    //========
    
    private RenderingPanel drawPanel;
    private JButton rebuild;

    public static BufferedImage world;
    public static BufferedImage worldBig;
    public static BufferedImage stars;
    
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
            worldBig = ImageIO.read(getClass().getResource("/world.topo.bathy.200408.3x21600x10800.png"));
            stars = ImageIO.read(getClass().getResource("/stars_darker.jpg"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        drawPanel = new RenderingPanel(1080, 720);
        
        rebuild = new JButton("Rebuild");
        LeftClick.applyOn(rebuild, () -> drawPanel.setup());
        rebuild.addKeyListener(drawPanel);
        
        add(rebuild, BorderLayout.NORTH);
        add(drawPanel, BorderLayout.CENTER);
        
        drawPanel.setup();
        setVisible(true);
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    public static Model loadModel(String fileName) {
        try {
            var loader = Thread.currentThread().getContextClassLoader();
            File file = new File(loader.getResource(fileName).toURI());
            
            if (!EFileUtil.fileExists(file)) return null;
            
            try (var r = new LineReader(file)) {
                EList<Triangle> loaded = EList.newList();
                EList<Vector3> verts = EList.newList();
                
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
                            verts.add(new Vector3(x, y, z));
                        }
                        if (parts[0].equals("f")) {
                            int f0 = Integer.parseInt(parts[1]);
                            int f1 = Integer.parseInt(parts[2]);
                            int f2 = Integer.parseInt(parts[3]);
                            Vertex v0 = new Vertex(verts.get(f0 - 1), new Vector2());
                            Vertex v1 = new Vertex(verts.get(f1 - 1), new Vector2());
                            Vertex v2 = new Vertex(verts.get(f2 - 1), new Vector2());
                            loaded.add(new Triangle(v0, v1, v2, Color.WHITE));
                        }
                    }
                }
                
                Model model = new Model() {};
                model.triangles.addAll(loaded);
                return model;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}