package controller.globe;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Cube extends Model {
    
    //========
    // Fields
    //========
    
    private Triangle s1;
    private Triangle s2;
    private Triangle e1;
    private Triangle e2;
    private Triangle n1;
    private Triangle n2;
    private Triangle w1;
    private Triangle w2;
    private Triangle u1;
    private Triangle u2;
    private Triangle l1;
    private Triangle l2;
    
    //==============
    // Constructors
    //==============
    
    public Cube() { this(1.0f, 1.0f, 1.0f); }
    public Cube(float w, float l, float h) {
        setup(w, l, h, null);
    }
    
    public Cube(BufferedImage texture) { this(1.0f, 1.0f, 1.0f, texture); }
    public Cube(float w, float l, float h, BufferedImage texture) {
        setup(w, l, h, texture);
    }
    
    //=========
    // Methods
    //=========
    
    public void setup(float w, float l, float h, BufferedImage texture) {
        triangles.clear();
        
        Vector3 b1 = new Vector3(0, 0, 0);
        Vector3 b2 = new Vector3(0, l, 0);
        Vector3 b3 = new Vector3(w, l, 0);
        Vector3 b4 = new Vector3(w, 0, 0);
        Vector3 t1 = new Vector3(0, 0, h);
        Vector3 t2 = new Vector3(0, l, h);
        Vector3 t3 = new Vector3(w, l, h);
        Vector3 t4 = new Vector3(w, 0, h);
        
        Vector2 a = new Vector2(0, 1);
        Vector2 b = new Vector2(0, 0);
        Vector2 c = new Vector2(1, 0);
        Vector2 d = new Vector2(1, 1);
        
        s1 = new Triangle(b1, b2, b3, a, b, c);
        s2 = new Triangle(b1, b3, b4, a, c, d);
        e1 = new Triangle(b4, b3, t3, a, b, c);
        e2 = new Triangle(b4, t3, t4, a, c, d);
        n1 = new Triangle(t4, t3, t2, a, b, c);
        n2 = new Triangle(t4, t2, t1, a, c, d);
        w1 = new Triangle(t1, t2, b2, a, b, c);
        w2 = new Triangle(t1, b2, b1, a, c, d);
        u1 = new Triangle(b2, t2, t3, a, b, c);
        u2 = new Triangle(b2, t3, b3, a, c, d);
        l1 = new Triangle(t4, t1, b1, a, b, c);
        l2 = new Triangle(t4, b1, b4, a, c, d);
        
        if (texture == null) {
            s1.color = Color.MAGENTA;
            s2.color = Color.MAGENTA;
            e1.color = Color.RED;
            e2.color = Color.RED;
            n1.color = Color.GREEN;
            n2.color = Color.GREEN;
            w1.color = Color.CYAN;
            w2.color = Color.CYAN;
            u1.color = Color.YELLOW;
            u2.color = Color.YELLOW;
            l1.color = Color.BLUE;
            l2.color = Color.BLUE;
        }
        else {
            setTexture(texture);
        }
        
        triangles.add(l1, l2, n1, n2, e1, e2, s1, s2, w1, w2, u1, u2);
    }
    
    public void setTexture(BufferedImage texture) {
        l1.setTexture(texture);
        l2.setTexture(texture);
        n1.setTexture(texture);
        n2.setTexture(texture);
        e1.setTexture(texture);
        e2.setTexture(texture);
        s1.setTexture(texture);
        s2.setTexture(texture);
        w1.setTexture(texture);
        w2.setTexture(texture);
        u1.setTexture(texture);
        u2.setTexture(texture);
    }
    
}
