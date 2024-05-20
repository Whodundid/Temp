package controller.globe;

import java.awt.image.BufferedImage;

public class Entity {
    
    //========
    // Fields
    //========
    
    public String name;
    public Model model;
    public BufferedImage texture;
    public Vector3 position = new Vector3();
    public Vector3 rotation = new Vector3();
    public Vector3 scale = new Vector3(1, 1, 1);
    
    //==============
    // Constructors
    //==============
    
    public Entity(Model model) { this(null, model); }
    public Entity(String name, Model model) {
        this.name = name;
        this.model = model;
    }
    
    //=========
    // Methods
    //=========
    
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    public void setRotationDegrees(float x, float y, float z) {
        rotation.set((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }
    
    //=========
    // Getters
    //=========
    
    public String getName() { return name; }
    public Model getModel() { return model; }
    
}
