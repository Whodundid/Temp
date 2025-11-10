package controller.globe.models;

import java.awt.Color;
import java.awt.image.BufferedImage;

import controller.globe.math.Vector2;
import controller.globe.math.Vector3;
import eutil.colors.EColors;

public class Sphere extends Model {
    
    //========
    // Fields
    //========
    
    public Color secondaryColor;
    
    public float radius;
    public int stackCount;
    public int sectorCount;
    
    public float sectorStep;
    public float stackStep;
    public float sectorAngle;
    public float stackAngle;    
    //==============
    // Constructors
    //==============
    
    public Sphere() { this(1.0f); }
    public Sphere(float radius) { this(radius, 18, 36); }
    public Sphere(float radius, int stacks, int sectors) {
        this(radius, stacks, sectors, EColors.dgray, EColors.gray);
    }
    
    public Sphere(Color color) { this(color, color); }
    public Sphere(Color a, Color b) { this(1.0f, a, b); }
    public Sphere(float radius, Color color) { this(radius, color, color); }
    public Sphere(float radius, Color a, Color b) { this(radius, 18, 36, a, b); }
    public Sphere(float radius, int stacks, int sectors, Color color) { this(radius, stacks, sectors, color, color); }
    public Sphere(float radius, int stacks, int sectors, Color a, Color b) {
        this.radius = radius;
        this.stackCount = stacks;
        this.sectorCount = sectors;
        this.color = a;
        this.secondaryColor = b;
        this.texture = null;
        setup();
    }

    public Sphere(BufferedImage texture) { this(1.0f, texture); }
    public Sphere(float radius, BufferedImage texture) { this(radius, 18, 36, texture); }
    public Sphere(float radius, int stacks, int sectors, BufferedImage texture) {
        this.radius = radius;
        this.stackCount = stacks;
        this.sectorCount = sectors;
        this.color = null;
        this.secondaryColor = null;
        this.texture = texture;
        setup();
    }    
    //===========
    // Overrides
    //===========
    
    @Override
    public Sphere copy() {
        Sphere c = new Sphere(radius, stackCount, sectorCount, color, secondaryColor);
        c.setTexture(texture);
        return c;
    }
    
    //=========
    // Methods
    //=========
    
    public void setup() {
        float PI = (float) Math.PI;
        float sectorStep = 2 * PI / sectorCount;
        float stackStep = PI / stackCount;
        
        for (int i = 0; i < stackCount; i++) {
            float stackAngleTop = PI / 2 - i * stackStep;
            float stackAngleBot = PI / 2 - (i + 1) * stackStep;
            float yTop = (float) (radius * Math.sin(stackAngleTop));
            float yBot = (float) (radius * Math.sin(stackAngleBot));
            
            for (int j = 0; j < sectorCount; j++) {
                float sectorAngle1 = j * sectorStep;
                float sectorAngle2 = (j + 1) * sectorStep;
                float x1 = (float) (radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle1));
                float z1 = (float) (radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle1));
                float x2 = (float) (radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle2));
                float z2 = (float) (radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle2));
                float x3 = (float) (radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle1));
                float z3 = (float) (radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle1));
                float x4 = (float) (radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle2));
                float z4 = (float) (radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle2));
                
                Vector3 v1 = new Vector3(x1, z1, yTop);
                Vector3 v2 = new Vector3(x2, z2, yTop);
                Vector3 v3 = new Vector3(x3, z3, yBot);
                Vector3 v4 = new Vector3(x4, z4, yBot);
                
                float s1 = (float) j / sectorCount;
                float t1 = (float) i / stackCount;
                float s2 = (float) (j + 1) / sectorCount;
                float t2 = (float) (i + 1) / stackCount;
                
                Vector2 a = new Vector2(s1, t2);
                Vector2 b = new Vector2(s1, t1);
                Vector2 c = new Vector2(s2, t1);
                Vector2 d = new Vector2(s2, t2);
                
                var tri1 = new Triangle(v1, v3, v2, b, a, c);
                var tri2 = new Triangle(v3, v4, v2, a, d, c);
                
                tri1.color = color;
                tri2.color = secondaryColor;
                
                triangles.add(tri1);
                triangles.add(tri2);
            }
        }
        
        if (texture != null) {
            setTexture(texture);
        }
    }
    
    @Override
    public void setTexture(BufferedImage tex) {
        if (tex == null) return;
        this.color = EColors.dgray;
        this.secondaryColor = EColors.gray;
        this.texture = tex;
        this.triangles.forEach(t -> t.setTexture(tex));
    }
    
    @Override
    public void setColor(Color color) {
        if (color == null) return;
        this.color = color;
        this.secondaryColor = color;
        this.texture = null;
        this.triangles.forEach(t -> t.color = color);
    }
    
    public void setColor(Color a, Color b) {
        if (a == null) return;
        this.color = a;
        this.secondaryColor = b;
        this.texture = null;
        for (int i = 0; i < triangles.size(); i++) {
            triangles.get(i).color = (i % 2 == 0) ? a : b;
        }
    }
    
    public void setRadius(float val) {
        radius = val;
        setup();
    }
    
    public void setSectorCount(int val) {
        sectorCount = val;
        setup();
    }
    
    public void setStackCount(int val) {
        stackCount = val;
        setup();
    }
    
}
