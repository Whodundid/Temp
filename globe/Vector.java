package controller.globe;

public class Vector {
    
    //========
    // Fields
    //========
    
    public float x;
    public float y;
    public float z;
    public float w;
    
    //==============
    // Constructors
    //==============
    
    public Vector() { this(0.0f, 0.0f, 0.0f, 1.0f); }
    public Vector(Vector v) { this(v.x, v.y, v.z, v.w); }
    public Vector(float x, float y, float z) { this(x, y, z, 1.0f); }
    public Vector(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        return "<" + x + ", " + y + ", " + z + ">";
    }
    
    //=========
    // Methods
    //=========
    
    public Vector add(Vector v) { return new Vector(x + v.x, y + v.y, z + v.z); }
    public Vector sub(Vector v) { return new Vector(x - v.x, y - v.y, z - v.z); }
    public Vector mul(float s) { return new Vector(x * s, y * s, z * s); }
    public Vector div(float s) { return new Vector(x / s, y / s, z / s); }
    public Vector norm() { float l = len(); return new Vector(x / l, y / l, z / l); }
    public Vector cross(Vector v) { return new Vector(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x); }
    public float dot(Vector v) { return x*v.x + y*v.y + z*v.z; }
    public float len() { return (float) Math.sqrt(dot(this)); }
    
    public static Vector intersectPlane(Vector p, Vector n, Vector lineStart, Vector lineEnd) {
        n = n.norm();
        float d = -n.dot(p);
        float ad = lineStart.dot(n);
        float bd = lineEnd.dot(n);
        float t = (-d - ad) / (bd - ad);
        Vector lineStartToEnd = lineEnd.sub(lineStart);
        Vector lineToIntersect = lineStartToEnd.mul(t);
        return lineStart.add(lineToIntersect);
    }
    
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
}
