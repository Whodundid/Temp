package controller.globe;

import java.awt.Color;

import eutil.datatypes.util.EList;

public class Cube extends Shape {
    
    //==============
    // Constructors
    //==============
    
    public Cube(float x, float y, float z, float w, float l, float h) {
        super(x, y, z);
        setup(x, y, z, w, l, h);
    }
    
    //=========
    // Methods
    //=========
    
    public void setup(float x, float y, float z, float w, float l, float h) {
        Vector b1 = new Vector(x, y, z);
        Vector b2 = new Vector(x, y + l, z);
        Vector b3 = new Vector(x + w, y + l, z);
        Vector b4 = new Vector(x + w, y, z);
        Vector t1 = new Vector(x, y, z + h);
        Vector t2 = new Vector(x, y + l, z + h);
        Vector t3 = new Vector(x + w, y + l, z + h);
        Vector t4 = new Vector(x + w, y, z + h);
        
        Triangle l1 = new Triangle(b1, b2, b4, Color.BLUE);
        Triangle l2 = new Triangle(b2, b3, b4, Color.BLUE);
        Triangle n1 = new Triangle(t2, b1, t1, Color.GREEN);
        Triangle n2 = new Triangle(t2, b2, b1, Color.GREEN);
        Triangle e1 = new Triangle(t3, b3, t2, Color.RED);
        Triangle e2 = new Triangle(t2, b3, b2, Color.RED);
        Triangle s1 = new Triangle(t4, b4, t3, Color.MAGENTA);
        Triangle s2 = new Triangle(t3, b4, b3, Color.MAGENTA);
        Triangle w1 = new Triangle(t1, b1, t4, Color.CYAN);
        Triangle w2 = new Triangle(t4, b1, b4, Color.CYAN);
        Triangle u1 = new Triangle(t4, t2, t1, Color.YELLOW);
        Triangle u2 = new Triangle(t4, t3, t2, Color.YELLOW);
        
        triangles.add(l1, l2, n1, n2, e1, e2, s1, s2, w1, w2, u1, u2);
    }
    
}
