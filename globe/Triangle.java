package controller.globe;

import java.awt.Color;
import java.awt.image.BufferedImage;

import eutil.datatypes.util.EList;

public class Triangle extends Model {
    
    //========
    // Fields
    //========
    
    public BufferedImage texture;
    public Vertex v0;
    public Vertex v1;
    public Vertex v2;
    public Color color = Color.WHITE;
    public float calculatedLighting = 1.0f;
    
    //==============
    // Constructors
    //==============
    
    public Triangle() {
        this(new Vertex(), new Vertex(), new Vertex(), Color.WHITE);
    }
    
    public Triangle(Triangle t) {
        v0 = new Vertex(t.v0);
        v1 = new Vertex(t.v1);
        v2 = new Vertex(t.v2);
        color = t.color;
        texture = t.texture;
        calculatedLighting = t.calculatedLighting;
    }
    
    public Triangle(Vertex v0, Vertex v1, Vertex v2, Color color) {
        set(v0, v1, v2, color);
        triangles.add(this);
    }
    
    public Triangle(Vertex v0, Vertex v1, Vertex v2, BufferedImage texture) {
        set(v0, v1, v2, texture);
        triangles.add(this);
    }
    
    public Triangle(Vector3 v0, Vector3 v1, Vector3 v2, Vector2 t0, Vector2 t1, Vector2 t2) {
        this(v0, v1, v2, t0, t1, t2, null);
    }
    
    public Triangle(Vector3 v0, Vector3 v1, Vector3 v2, Vector2 t0, Vector2 t1, Vector2 t2, BufferedImage texture) {
        this.v0 = new Vertex(v0, t0);
        this.v1 = new Vertex(v1, t1);
        this.v2 = new Vertex(v2, t2);
        this.texture = texture;
    }
    
    public Triangle(float v0x, float v0y, float v0z,
                    float v1x, float v1y, float v1z,
                    float v2x, float v2y, float v2z,
                    float t0u, float t0v,
                    float t1u, float t1v,
                    float t2u, float t2v,
                    BufferedImage texture)
    {
        v0 = new Vertex(v0x, v0y, v0z, t0u, t0v);
        v1 = new Vertex(v1x, v1y, v1z, t1u, t1v);
        v2 = new Vertex(v2x, v2y, v2z, t2u, t2v);
        this.texture = texture;
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        return "T[" + v0 + ";" + v1 + ";" + v2 + ";" + color + "]";
    }
    
    //=========
    // Getters
    //=========
    
    public Vector3 v0() { return v0.pos; }
    public Vector3 v1() { return v1.pos; }
    public Vector3 v2() { return v2.pos; }
    public Vector2 t0() { return v0.tex; }
    public Vector2 t1() { return v1.tex; }
    public Vector2 t2() { return v2.tex; }
    
    public float v0x() { return v0.pos.x; }
    public float v0y() { return v0.pos.y; }
    public float v0z() { return v0.pos.z; }
    public float v0w() { return v0.pos.w; }
    public float v1x() { return v1.pos.x; }
    public float v1y() { return v1.pos.y; }
    public float v1z() { return v1.pos.z; }
    public float v1w() { return v1.pos.w; }
    public float v2x() { return v2.pos.x; }
    public float v2y() { return v2.pos.y; }
    public float v2z() { return v2.pos.z; }
    public float v2w() { return v2.pos.w; }
    
    public float t0x() { return v0.tex.x; }
    public float t0y() { return v0.tex.y; }
    public float t0w() { return v0.tex.w; }
    public float t1x() { return v1.tex.x; }
    public float t1y() { return v1.tex.y; }
    public float t1w() { return v1.tex.w; }
    public float t2x() { return v2.tex.x; }
    public float t2y() { return v2.tex.y; }
    public float t2w() { return v2.tex.w; }
    
    //=========
    // Setters
    //=========
    
    public void set(Triangle t) {
        v0 = new Vertex(t.v0);
        v1 = new Vertex(t.v1);
        v2 = new Vertex(t.v2);
        color = t.color;
        texture = t.texture;
    }
    
    public void set(Vertex v0, Vertex v1, Vertex v2, Color color) {
        this.v0 = new Vertex(v0);
        this.v1 = new Vertex(v1);
        this.v2 = new Vertex(v2);
        this.color = color;
    }
    
    public void set(Vertex v0, Vertex v1, Vertex v2, BufferedImage texture) {
        this.v0 = new Vertex(v0);
        this.v1 = new Vertex(v1);
        this.v2 = new Vertex(v2);
        this.color = Color.WHITE;
        this.texture = texture;
    }
    
    public void setTexture(BufferedImage texture) {
        this.texture = texture;
    }
    
    //=========
    // Methods
    //=========
    
    public Vector3 calculateNormal(Vector3 cameraPos) {
        // calculate normal
        Vector3 line1 = v1.pos.sub(v0.pos);
        Vector3 line2 = v2.pos.sub(v0.pos);
        return line1.cross(line2).norm();
    }
    
    public float cameraRay(Vector3 normal, Vector3 cameraPos) {
        // get ray from triangle to camera
        Vector3 cameraRay = v0.pos.sub(cameraPos);
        return normal.dot(cameraRay);
    }
    
    public Triangle positionInWindow(Vector3 offsetView, float width, float height) {
        // scale into view
        v0.pos = v0.pos.div(v0.pos.w);
        v1.pos = v1.pos.div(v1.pos.w);
        v2.pos = v2.pos.div(v2.pos.w);
        // x/y are inverted so put them back
        v0.pos.x *= -1.0f; v0.pos.y *= -1.0f;
        v1.pos.x *= -1.0f; v1.pos.y *= -1.0f;
        v2.pos.x *= -1.0f; v2.pos.y *= -1.0f;
        // offset verts into visible normalized space
        v0.pos = v0.pos.add(offsetView);
        v1.pos = v1.pos.add(offsetView);
        v2.pos = v2.pos.add(offsetView);
        v0.pos.x *= 0.5f * width; v0.pos.y *= 0.5f * height;
        v1.pos.x *= 0.5f * width; v1.pos.y *= 0.5f * height;
        v2.pos.x *= 0.5f * width; v2.pos.y *= 0.5f * height;
        return this;
    }
    
    public Triangle[] clipAgainstPlane(Vector3 p, Vector3 n) {
        // make sure plane is normal
        n = n.norm();
        
        // return signed shortest distance from point to plane, plane normal must be normalized
        float dotNP = n.dot(p);
        float d0 = (n.x * v0.pos.x + n.y * v0.pos.y + n.z * v0.pos.z - dotNP);
        float d1 = (n.x * v1.pos.x + n.y * v1.pos.y + n.z * v1.pos.z - dotNP);
        float d2 = (n.x * v2.pos.x + n.y * v2.pos.y + n.z * v2.pos.z - dotNP);
        
        Vector3[] vin = new Vector3[3];
        Vector2[] tin = new Vector2[3];
        Vector3[] vout = new Vector3[3];
        Vector2[] tout = new Vector2[3];
        int vInCount = 0, vOutount = 0;
        int tInCount = 0, tOutCount = 0;
        
        if (d0 >= 0) {  vin[vInCount++] = v0.pos;   tin[tInCount++] = v0.tex; }
                else { vout[vOutount++] = v0.pos; tout[tOutCount++] = v0.tex; }
        if (d1 >= 0) {  vin[vInCount++] = v1.pos;   tin[tInCount++] = v1.tex; }
                else { vout[vOutount++] = v1.pos; tout[tOutCount++] = v1.tex; }
        if (d2 >= 0) {  vin[vInCount++] = v2.pos;   tin[tInCount++] = v2.tex; }
                else { vout[vOutount++] = v2.pos; tout[tOutCount++] = v2.tex; }
        
        // triangle should be clipped as two points lie outside the plane, the triangle becomes smaller
        if (vInCount == 1) {
            Triangle out = new Triangle();
            out.v0.pos = vin[0];
            out.v0.tex = tin[0];
            out.color = color;
            out.texture = texture;
            out.calculatedLighting = calculatedLighting;
            float t1 = Vector3.calculateIntersectionPoint(p, n, vin[0], vout[0]);
            out.v1.pos = Vector3.intersectPlane(vin[0], vout[0], t1);
            out.v1.tex.x = t1 * (tout[0].x - tin[0].x) + tin[0].x;
            out.v1.tex.y = t1 * (tout[0].y - tin[0].y) + tin[0].y;
            out.v1.tex.w = t1 * (tout[0].w - tin[0].w) + tin[0].w;
            float t2 = Vector3.calculateIntersectionPoint(p, n, vin[0], vout[1]);
            out.v2.pos = Vector3.intersectPlane(vin[0], vout[1], t2);
            out.v2.tex.x = t2 * (tout[1].x - tin[0].x) + tin[0].x;
            out.v2.tex.y = t2 * (tout[1].y - tin[0].y) + tin[0].y;
            out.v2.tex.w = t2 * (tout[1].w - tin[0].w) + tin[0].w;
            
            return new Triangle[] { out };
        }
        // triangle should be clipped as two points lie inside the plane, triangle becomes a 'quad'
        if (vInCount == 2) {
            Triangle out1 = new Triangle(this);
            out1.v0.pos = vin[0];
            out1.v0.tex = tin[0];
            out1.v1.pos = vin[1];
            out1.v1.tex = tin[1];
            out1.color = color;
            out1.texture = texture;
            out1.calculatedLighting = calculatedLighting;
            float t1 = Vector3.calculateIntersectionPoint(p, n, vin[0], vout[0]);
            out1.v2.pos = Vector3.intersectPlane(vin[0], vout[0], t1);
            out1.v2.tex.x = t1 * (tout[0].x - tin[0].x) + tin[0].x;
            out1.v2.tex.y = t1 * (tout[0].y - tin[0].y) + tin[0].y;
            out1.v2.tex.w = t1 * (tout[0].w - tin[0].w) + tin[0].w;
            
            Triangle out2 = new Triangle(this);
            out2.v0.pos = vin[1];
            out2.v0.tex = tin[1];
            out2.v1.pos = new Vector3(out1.v2.pos);
            out2.v1.tex = new Vector2(out1.v2.tex);
            out2.color = color;
            out2.texture = texture;
            out2.calculatedLighting = calculatedLighting;
            float t2 = Vector3.calculateIntersectionPoint(p, n, vin[1], vout[0]);
            out2.v2.pos = Vector3.intersectPlane(vin[1], vout[0], t2);
            out2.v2.tex.x = t2 * (tout[0].x - tin[1].x) + tin[1].x;
            out2.v2.tex.y = t2 * (tout[0].y - tin[1].y) + tin[1].y;
            out2.v2.tex.w = t2 * (tout[0].w - tin[1].w) + tin[1].w;
            
            return new Triangle[] { out1, out2 };
        }
        // all points lie on the inside of plane, so do nothing and allow the triangle to pass
        if (vInCount == 3) {
            return new Triangle[] { this };
        }
        
        // all points lie on the outside of the plane, so clip the whole triangle
        return new Triangle[0];
    }
    
    public EList<Triangle> clipAgainstScreen(int screenWidth, int screenHeight) {        
        EList<Triangle> clippedTriangles = EList.of(this);
        int newTriangles = 1;
        
        for (int p = 0; p < 4; p++) {
            while (newTriangles > 0) {
                // take triangle from front of queue
                Triangle[] clipped = null;
                Triangle t = clippedTriangles.pop();
                newTriangles--;
                
                // clip it against a plane
                switch (p) {
                case 0: clipped = t.clipAgainstPlane(new Vector3(0.0f, 0.0f, 0.0f), new Vector3(0.0f, 1.0f, 0.0f)); break;
                case 1: clipped = t.clipAgainstPlane(new Vector3(0.0f, screenHeight - 1, 0.0f), new Vector3(0.0f, -1.0f, 0.0f)); break;
                case 2: clipped = t.clipAgainstPlane(new Vector3(0.0f, 0.0f, 0.0f), new Vector3(1.0f, 0.0f, 0.0f)); break;
                case 3: clipped = t.clipAgainstPlane(new Vector3(screenWidth - 1, 0.0f, 0.0f), new Vector3(-1.0f, 0.0f, 0.0f)); break;
                }
                
                // clipping may yield a variable number of triangles, so add these new ones
                // to the back of the queue for subsequent clipping against next planes
                if (clipped == null) continue;
                for (int w = 0; w < clipped.length; w++) {
                    clippedTriangles.add(clipped[w]);
                }
            }
            newTriangles = clippedTriangles.size();
        }
        
        return clippedTriangles;
    }
    
}
