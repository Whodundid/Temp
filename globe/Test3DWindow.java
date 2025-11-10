package controller.globe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import controller.globe.models.Model;
import controller.globe.models.Triangle;
import eutil.datatypes.util.EList;
import eutil.file.EFileUtil;
import eutil.file.LineReader;
import eutil.swing.components.EButton;
import eutil.swing.listeners.LeftPress;

public class Test3DWindow extends JFrame {
    
    //========
    // Fields
    //========
    
    private RenderingPanel drawPanel;
    private EButton rebuild;

    public static BufferedImage world;
    public static BufferedImage worldBig;
    public static BufferedImage stars;
    public static BufferedImage moon;
    public static BufferedImage sun;
    public static BufferedImage mercury;
    public static BufferedImage venus;
    public static BufferedImage mars;
    public static BufferedImage jupiter;
    public static BufferedImage saturn;
    public static BufferedImage uranus;
    public static BufferedImage neptune;    
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
//            moon = ImageIO.read(getClass().getResource("/2k_moon.jpg"));
//            sun = ImageIO.read(getClass().getResource("/2k_sun.jpg"));
//            mercury = ImageIO.read(getClass().getResource("/2k_mercury.jpg"));
//            venus = ImageIO.read(getClass().getResource("/2k_venus_atmosphere.jpg"));
//            mars = ImageIO.read(getClass().getResource("/2k_mars.jpg"));
//            jupiter = ImageIO.read(getClass().getResource("/2k_jupiter.jpg"));
//            saturn = ImageIO.read(getClass().getResource("/2k_saturn.jpg"));
//            uranus = ImageIO.read(getClass().getResource("/2k_uranus.jpg"));
//            neptune = ImageIO.read(getClass().getResource("/2k_neptune.jpg"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        drawPanel = new RenderingPanel(1080, 720);
        
        rebuild = new EButton("Rebuild", drawPanel::setup);
        rebuild.addKeyListener(drawPanel);
        
        add(rebuild, BorderLayout.NORTH);
        add(drawPanel, BorderLayout.CENTER);
        
        drawPanel.setup();
        setVisible(true);
    }
    
}