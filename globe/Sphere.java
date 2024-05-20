package controller.globe;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Sphere extends Model {
    
    //========
    // Fields
    //========
    
    public int stackCount = 30;
    public int sectorCount = 50;
    
    //==============
    // Constructors
    //==============
    
    public Sphere() { this(1.0f, null); }
    public Sphere(float radius) { this(radius, null); }
    public Sphere(BufferedImage texture) { this(1.0f, texture); }
    public Sphere(float radius, BufferedImage texture) { this(radius, 18, 36, texture); }
    public Sphere(float radius, int stacks, int sectors, BufferedImage texture) {
        stackCount = stacks;
        sectorCount = sectors;
        setup(radius, texture);
    }
    
    //=========
    // Methods
    //=========
    
    public void setup(float radius, BufferedImage texture) {
        float PI = (float) Math.PI;
        float sectorStep = 2 * PI / sectorCount;
        float stackStep = PI / stackCount;
        
        float radiusZ = radius * 0.997f;
        
        for (int i = 0; i < stackCount; i++) {
            float stackAngleTop = PI / 2 - i * stackStep;
            float stackAngleBot = PI / 2 - (i + 1) * stackStep;
            float zTop = (float) (radiusZ * Math.sin(stackAngleTop));
            float zBot = (float) (radiusZ * Math.sin(stackAngleBot));
            
            for (int j = 0; j < sectorCount; j++) {
                float sectorAngle1 = j * sectorStep;
                float sectorAngle2 = (j + 1) * sectorStep;
                float x1 = (float) (radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle1));
                float y1 = (float) (radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle1));
                float x2 = (float) (radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle2));
                float y2 = (float) (radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle2));
                float x3 = (float) (radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle1));
                float y3 = (float) (radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle1));
                float x4 = (float) (radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle2));
                float y4 = (float) (radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle2));
                
                Vector3 v1 = new Vector3(x1, y1, zTop);
                Vector3 v2 = new Vector3(x2, y2, zTop);
                Vector3 v3 = new Vector3(x3, y3, zBot);
                Vector3 v4 = new Vector3(x4, y4, zBot);
                
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
                
                tri1.color = Color.WHITE;
                tri2.color = Color.GRAY;
                
                triangles.add(tri1);
                triangles.add(tri2);
            }
        }
        
        if (texture != null) {
            setTexture(texture);
        }
    }
    
    public void setTexture(BufferedImage texture) {
        triangles.forEach(t -> t.setTexture(texture));
    }
    
}
