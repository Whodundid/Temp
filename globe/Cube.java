package controller.globe;

import java.awt.Color;

import eutil.datatypes.util.EList;

public class Cube {
    
    //========
    // Fields
    //========
    
    public EList<Triangle> triangles = EList.newList();
    
    //==============
    // Constructors
    //==============
    
    public Cube(double x, double y, double z, double w, double l, double h) {
        setup(x, y, z, w, l, h);
    }
    
    //=========
    // Methods
    //=========
    
    public void setup(double x, double y, double z, double w, double l, double h) {
        Vertex b1 = new Vertex(x, y, z);
        Vertex b2 = new Vertex(x, y + l, z);
        Vertex b3 = new Vertex(x + w, y + l, z);
        Vertex b4 = new Vertex(x + w, y, z);
        Vertex t1 = new Vertex(x, y, z + h);
        Vertex t2 = new Vertex(x, y + l, z + h);
        Vertex t3 = new Vertex(x + w, y + l, z + h);
        Vertex t4 = new Vertex(x + w, y, z + h);
        
        Triangle l1 = new Triangle(b1, b4, b2, Color.BLUE);
        Triangle l2 = new Triangle(b2, b4, b3, Color.BLUE);
        Triangle n1 = new Triangle(t1, b1, t2, Color.GREEN);
        Triangle n2 = new Triangle(t2, b1, b2, Color.GREEN);
        Triangle e1 = new Triangle(t2, b3, t3, Color.RED);
        Triangle e2 = new Triangle(t2, b2, b3, Color.RED);
        Triangle s1 = new Triangle(t3, b4, t4, Color.MAGENTA);
        Triangle s2 = new Triangle(t3, b3, b4, Color.MAGENTA);
        Triangle w1 = new Triangle(t4, b1, t1, Color.CYAN);
        Triangle w2 = new Triangle(t4, b4, b1, Color.CYAN);
        Triangle u1 = new Triangle(t4, t2, t1, Color.YELLOW);
        Triangle u2 = new Triangle(t4, t3, t2, Color.YELLOW);
        
        triangles.add(l1, l2, n1, n2, e1, e2, s1, s2, w1, w2, u1, u2);
    }
    
}
