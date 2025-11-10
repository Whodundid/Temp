package controller.globe.util;

import java.awt.image.BufferedImage;

public class Texture {
    
    //========
    // Fields
    //========
    
    public final String path;
    public final BufferedImage image;
    
    //==============
    // Constructors
    //==============
    
    public Texture(String path, BufferedImage image) {
        this.path = path;
        this.image = image;
    }
    
}
