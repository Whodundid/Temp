package controller.globe;

public class Vector2 {
    
    //========
    // Fields
    //========
    
    public float x;
    public float y;
    public float w;
    
    //==============
    // Constructors
    //==============
    
    public Vector2() { this(0.0f, 0.0f, 1.0f); }
    public Vector2(Vector2 v) { this(v.x, v.y, v.w); }
    public Vector2(float x, float y) { this(x, y, 1.0f); }
    public Vector2(float x, float y, float w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        return "<" + x + ", " + y + ">";
    }
    
    //=========
    // Methods
    //=========
    
    public Vector2 add(Vector2 v) { return new Vector2(x + v.x, y + v.y); }
    public Vector2 sub(Vector2 v) { return new Vector2(x - v.x, y - v.y); }
    public Vector2 mul(float s) { return new Vector2(x * s, y * s); }
    public Vector2 div(float s) { return new Vector2(x / s, y / s); }
    public Vector2 norm() { float l = len(); return new Vector2(x / l, y / l); }
    public float dot(Vector2 v) { return x*v.x + y*v.y; }
    public float len() { return (float) Math.sqrt(dot(this)); }
    
    public static Vector2 intersectPlane(Vector2 p, Vector2 n, Vector2 lineStart, Vector2 lineEnd) {
        n = n.norm();
        float d = -n.dot(p);
        float ad = lineStart.dot(n);
        float bd = lineEnd.dot(n);
        float t = (-d - ad) / (bd - ad);
        Vector2 lineStartToEnd = lineEnd.sub(lineStart);
        Vector2 lineToIntersect = lineStartToEnd.mul(t);
        return lineStart.add(lineToIntersect);
    }
    
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
    }
    
}
