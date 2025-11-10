package controller.globe.models;

import java.awt.Color;
import java.awt.image.BufferedImage;

import controller.globe.math.Vector2;
import controller.globe.math.Vector3;

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
    
    // because a cube cannot be drawn in just one line, have to break it into multiple
    // in this case, there are 4 3d 'C's that are combined to make the outline
    private Line3D outlineA;
    private Line3D outlineB;
    private Line3D outlineC;
    private Line3D outlineD;
    
    public float width;
    public float length;
    public float height;
    public boolean drawOutline;
    public int outlineWidth = 2;    
    //==============
    // Constructors
    //==============
    
    public Cube() { this(1.0f, 1.0f, 1.0f, (Color) null, false); }
    public Cube(boolean drawOutline) { this(1.0f, 1.0f, 1.0f, (Color) null, drawOutline); }
    public Cube(Color color) { this(1.0f, 1.0f, 1.0f, color, false); }
    public Cube(Color color, boolean drawOutline) { this(1.0f, 1.0f, 1.0f, color, drawOutline); }
    public Cube(float w, float l, float h) { this(w, l, h, (Color) null, false); }
    public Cube(float w, float l, float h, Color color) { this(w, l, h, color, false); }
    public Cube(float w, float l, float h, Color color, boolean drawOutline) {
        this.width = w;
        this.length = l;
        this.height = h;
        this.color = color;
        this.texture = null;
        this.drawOutline = drawOutline;
        setup();
    }
    
    public Cube(BufferedImage texture) { this(1.0f, 1.0f, 1.0f, texture, false); }
    public Cube(BufferedImage texture, boolean drawOutline) { this(1.0f, 1.0f, 1.0f, texture, drawOutline); }
    public Cube(float w, float l, float h, BufferedImage texture) { this(1.0f, 1.0f, 1.0f, texture, false); }    public Cube(float w, float l, float h, BufferedImage texture, boolean drawOutline) {
        this.width = w;
        this.length = l;
        this.height = h;
        this.color = null;
        this.texture = texture;
        this.drawOutline = drawOutline;
        setup();
    }
    
    //===========
    // Overrides
    //===========
    
    public Cube copy() {
        Cube c = new Cube(width, length, height);
        c.color = color;
        c.texture = texture;
        if (texture != null) {
            c.setTexture(texture);
        }
        c.drawFilled = drawFilled;
        c.drawOutline = drawOutline;
        return c;
    }
    
    //=========
    // Methods
    //=========
    
    protected void setup() {
        triangles.clear();
        float hw = width * 0.5f;
        float hl = length * 0.5f;
        float hh = height * 0.5f;
        
        Vector3 b1 = new Vector3(-hw, -hl, -hh);
        Vector3 b2 = new Vector3(-hw,  hl, -hh);
        Vector3 b3 = new Vector3( hw,  hl, -hh);
        Vector3 b4 = new Vector3( hw, -hl, -hh);
        Vector3 t1 = new Vector3(-hw, -hl,  hh);
        Vector3 t2 = new Vector3(-hw,  hl,  hh);
        Vector3 t3 = new Vector3( hw,  hl,  hh);
        Vector3 t4 = new Vector3( hw, -hl,  hh);
        
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
        
        if (texture != null) {
            setTexture(texture);
        }
        else if (color != null) {
            s1.color = color; 
            s2.color = color; 
            e1.color = color; 
            e2.color = color; 
            n1.color = color; 
            n2.color = color; 
            w1.color = color; 
            w2.color = color; 
            u1.color = color; 
            u2.color = color; 
            l1.color = color; 
            l2.color = color; 
        }
        else {
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
        
        triangles.add(l1, l2, n1, n2, e1, e2, s1, s2, w1, w2, u1, u2);
        
        if (drawOutline) {
            outlineA = new Line3D(b1, b2, t2, t1);
            outlineB = new Line3D(b2, b3, t3, t2);
            outlineC = new Line3D(b3, b4, t4, t3);
            outlineD = new Line3D(b4, b1, t1, t4);
            
            outlineA.lineWidth = outlineWidth;
            outlineB.lineWidth = outlineWidth;
            outlineC.lineWidth = outlineWidth;
            outlineD.lineWidth = outlineWidth;
            
            childModels.add(outlineA, outlineB, outlineC, outlineD);
            outlineA.visible = drawOutline;
            outlineB.visible = drawOutline;
            outlineC.visible = drawOutline;
            outlineD.visible = drawOutline;
        }
        else {
            outlineA = null;
            outlineB = null;
            outlineC = null;
            outlineD = null;
        }
    }
    
    @Override
    public void setColor(Color color) {
        this.color = color;
        this.texture = null;
        
        for (var t : triangles) {
            t.color = color;
            t.texture = null;
        }
    }
    
    @Override
    public void setTexture(BufferedImage texture) {
        this.color = null;
        this.texture = texture;
        
        for (var t : triangles) {
            t.color = null;
            t.texture = texture;
        }
    }
    
    public void setDrawOutline(boolean val) {
        this.drawOutline = val;
        setup();
    }
    
    public void setOutlineWidth(int width) {
        this.outlineWidth = width;
        if (outlineA != null) outlineA.lineWidth = outlineWidth;
        if (outlineB != null) outlineB.lineWidth = outlineWidth;
        if (outlineC != null) outlineC.lineWidth = outlineWidth;
        if (outlineD != null) outlineD.lineWidth = outlineWidth;
    }
    
    public void setOutlineColor(Color color) {
        if (color == null) return;
        if (outlineA != null) outlineA.color = color;
        if (outlineB != null) outlineB.color = color;
        if (outlineC != null) outlineC.color = color;
        if (outlineD != null) outlineD.color = color;
    }
    
    public void setTopColor(Color color) {
        u1.color = color;
        u2.color = color;
    }
    
    public void setBottomColor(Color color) {
        l1.color = color;
        l2.color = color;
    }
    
    public void setNorthColor(Color color) {
        n1.color = color;
        n2.color = color;
    }
    
    public void setEastColor(Color color) {
        e1.color = color;
        e2.color = color;
    }
    
    public void setSouthColor(Color color) {
        s1.color = color;
        s2.color = color;
    }
    
    public void setWestColor(Color color) {
        w1.color = color;
        w2.color = color;
    }
    
}
