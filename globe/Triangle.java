package controller.globe;

import java.awt.Color;

public class Triangle extends Shape {
    
    public Vector v0;
    public Vector v1;
    public Vector v2;
    public Color color;
    
    public Triangle() {
        this(new Vector(), new Vector(), new Vector(), Color.WHITE);
    }
    
    public Triangle(Triangle t) {
        this(t.v0, t.v1, t.v2, t.color);
    }
    
    public Triangle(Vector v0, Vector v1, Vector v2, Color color) {
        set(v0, v1, v2, color);
        triangles.add(this);
    }
    
    @Override
    public String toString() {
        return "T[" + v0 + ";" + v1 + ";" + v2 + ";" + color + "]";
    }
    
    public void set(Triangle t) {
        v0 = new Vector(t.v0);
        v1 = new Vector(t.v1);
        v2 = new Vector(t.v2);
        color = t.color;
    }
    
    public void set(Vector v0, Vector v1, Vector v2, Color color) {
        this.v0 = new Vector(v0);
        this.v1 = new Vector(v1);
        this.v2 = new Vector(v2);
        this.color = color;
    }
    
    public Triangle[] clipAgainstPlane(Vector p, Vector n) {
        // make sure plane is normal
        n = n.norm();
        
        // return signed shortest distance from point to plane, plane normal must be normalized
        float dotNP = n.dot(p);
        float d0 = (n.x * v0.x + n.y * v0.y + n.z * v0.z - dotNP);
        float d1 = (n.x * v1.x + n.y * v1.y + n.z * v1.z - dotNP);
        float d2 = (n.x * v2.x + n.y * v2.y + n.z * v2.z - dotNP);
        
        Vector[] in = new Vector[3];
        Vector[] out = new Vector[3];
        int insideCount = 0;
        int outsideCount = 0;
        
        if (d0 >= 0) in[insideCount++] = v0; else out[outsideCount++] = v0;
        if (d1 >= 0) in[insideCount++] = v1; else out[outsideCount++] = v1;
        if (d2 >= 0) in[insideCount++] = v2; else out[outsideCount++] = v2;
        
        // triangle should be clipped as two points lie outside the plane, the triangle becomes smaller
        if (insideCount == 1) {
            Vector v1 = Vector.intersectPlane(p, n, in[0], out[0]);
            Vector v2 = Vector.intersectPlane(p, n, in[0], out[1]);
            return new Triangle[] { new Triangle(in[0], v1, v2, color) };
        }
        // triangle should be clipped as two points lie inside the plane, triangle becomes a 'quad'
        if (insideCount == 2) {
            var t1 = new Triangle(in[0], in[1], Vector.intersectPlane(p, n, in[0], out[0]), color);
            var t2 = new Triangle(in[1], t1.v2, Vector.intersectPlane(p, n, in[1], out[0]), color);
            return new Triangle[] { t1, t2 };
        }
        // all points lie on the inside of plane, so do nothing and allow the triangle to pass
        if (insideCount == 3) {
            return new Triangle[] { this };
        }
        
        // all points lie on the outside of the plane, so clip the whole triangle
        return new Triangle[0];
    }
    
}
