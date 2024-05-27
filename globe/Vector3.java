package controller.globe;

import java.text.DecimalFormat;

public class Vector3 {
    
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
    
    public Vector3() { this(0.0f, 0.0f, 0.0f, 1.0f); }
    public Vector3(Vector3 v) { this(v.x, v.y, v.z, v.w); }
    public Vector3(float x, float y, float z) { this(x, y, z, 1.0f); }
    public Vector3(float x, float y, float z, float w) {
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
        var df = new DecimalFormat("#.##");
        return "<" + df.format(x) + ", " + df.format(y) + ", " + df.format(z) + ">";
    }
    
    //=========
    // Methods
    //=========
    
    public Vector3 addT(float x, float y, float z) { this.x += x; this.y += y; this.z += z; return this; }
    public Vector3 addT(Vector3 v) { x += v.x; y += v.y; z += v.z; return this; }
    public Vector3 subT(float x, float y, float z) { this.x -= x; this.y -= y; this.z -= z; return this; }
    public Vector3 subT(Vector3 v) { x -= v.x; y -= v.y; z -= v.z; return this; }
    
    public Vector3 add(float x, float y, float z) { return new Vector3(this.x + x, this.y + y, this.z + z); }
    public Vector3 add(Vector3 v) { return new Vector3(x + v.x, y + v.y, z + v.z); }
    public Vector3 sub(float x, float y, float z) { return new Vector3(this.x - x, this.y - y, this.z - z); }
    public Vector3 sub(Vector3 v) { return new Vector3(x - v.x, y - v.y, z - v.z); }
    public Vector3 mul(float s) { return new Vector3(x * s, y * s, z * s); }
    public Vector3 div(float s) { return new Vector3(x / s, y / s, z / s); }
    public Vector3 norm() { float l = len(); return new Vector3(x / l, y / l, z / l); }
    public Vector3 cross(Vector3 v) { return new Vector3(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x); }
    public float dot(Vector3 v) { return x*v.x + y*v.y + z*v.z; }
    public float len() { return (float) Math.sqrt(dot(this)); }
    
    public static float calculateIntersectionPoint(Vector3 plane, Vector3 normal, Vector3 lineStart, Vector3 lineEnd) {
        normal = normal.norm();
        float d = -normal.dot(plane);
        float ad = lineStart.dot(normal);
        float bd = lineEnd.dot(normal);
        return (-d - ad) / (bd - ad);
    }
    
    public static Vector3 intersectPlane(Vector3 plane, Vector3 normal, Vector3 lineStart, Vector3 lineEnd) {
        float t = calculateIntersectionPoint(plane, normal, lineStart, lineEnd);
        return intersectPlane(lineStart, lineEnd, t);
    }
    
    public static Vector3 intersectPlane(Vector3 lineStart, Vector3 lineEnd, float intersectionPoint) {
        Vector3 lineStartToEnd = lineEnd.sub(lineStart);
        Vector3 lineToIntersect = lineStartToEnd.mul(intersectionPoint);
        return lineStart.add(lineToIntersect);
    }
    
    public void set(Vector3 v) { set(v.x, v.y, v.z); }
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
}
