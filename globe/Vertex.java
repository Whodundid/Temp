package controller.globe;

public class Vertex {
    
    //========
    // Fields
    //========
    
    public Vector3 pos;
    public Vector2 tex;
    
    //==============
    // Constructors
    //==============
    
    public Vertex() {
        this(new Vector3(), new Vector2());
    }
    
    public Vertex(Vertex v) {
        this(new Vector3(v.pos), new Vector2(v.tex));
    }
    
    public Vertex(Vector3 v) {
        this(new Vector3(v), new Vector2());
    }
    
    public Vertex(float x, float y, float z) {
        this(new Vector3(x, y, z), new Vector2());
    }
    
    public Vertex(float x, float y, float z, float u, float v) {
        this(new Vector3(x, y, z), new Vector2(u, v));
    }
    
    public Vertex(Vector3 position, Vector2 texture) {
        this.pos = position;
        this.tex = texture;
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        return "<" + pos.x + ", " + pos.y + ", " + pos.z + ">";
    }
    
}
