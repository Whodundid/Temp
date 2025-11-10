package controller.globe.math;

public class Vertex {
    
    //========
    // Fields
    //========
    
    /** Position (object/world space depending on stage) */
    public Vector3 pos;
    /** Texture coordinate (u, v) */
    public Vector2 tex;
    /** Normal (object/world space depending on stage). May be null or (0,0,0) if not provided. */
    public Vector3 norm;    
    //==============
    // Constructors
    //==============
    
    public Vertex() {
        this(new Vector3(), new Vector2(), new Vector3());
    }

    public Vertex(Vertex v) {
        this(new Vector3(v.pos), new Vector2(v.tex), v.norm != null ? new Vector3(v.norm) : new Vector3());
    }

    public Vertex(Vector3 position) {
        this(new Vector3(position), new Vector2(), new Vector3());
    }

    public Vertex(float x, float y, float z) {
        this(new Vector3(x, y, z), new Vector2(), new Vector3());
    }

    public Vertex(float x, float y, float z, float u, float v) {
        this(new Vector3(x, y, z), new Vector2(u, v), new Vector3());
    }

    public Vertex(Vector3 position, Vector2 texture) {
        this(position, texture, new Vector3());
    }

    public Vertex(Vector3 position, Vector2 texture, Vector3 normal) {
        this.pos  = position  != null ? position  : new Vector3();
        this.tex  = texture   != null ? texture   : new Vector2();
        this.norm = normal    != null ? normal    : new Vector3();
    }    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        String n = (norm != null) ? (", n=<" + norm.x + "," + norm.y + "," + norm.z + ">") : "";
        return "<" + pos.x + "," + pos.y + "," + pos.z + ">, uv=<" + tex.x + "," + tex.y + ">" + n;
    }
    
    //=========
    // Setters
    //=========
    
    public Vertex setPos(float x, float y, float z) { this.pos.set(x, y, z); return this; }
    public Vertex setPos(Vector3 p)                 { this.pos.set(p);      return this; }
    public Vertex setTex(float u, float v)          { this.tex.set(u, v);   return this; }
    public Vertex setTex(Vector2 t)                 { this.tex.set(t);      return this; }
    public Vertex setNormal(float x, float y, float z) {
        if (this.norm == null) this.norm = new Vector3();
        this.norm.set(x, y, z);
        return this;
    }
    public Vertex setNormal(Vector3 n) {
        if (n == null) return this;
        if (this.norm == null) this.norm = new Vector3();
        this.norm.set(n);
        return this;
    }

    public boolean hasNormal() {
        return norm != null && !(norm.x == 0f && norm.y == 0f && norm.z == 0f);
    }

    public Vertex normalizeNormal() {
        if (norm != null) norm.normalize();
        return this;
    }

    /** Shallow-ish copy of value state (no shared vectors). */
    public Vertex copy() {
        return new Vertex(new Vector3(pos), new Vector2(tex), norm != null ? new Vector3(norm) : new Vector3());
    }

    /** Convenience: return a new vertex same as this but with a different normal. */
    public Vertex withNormal(Vector3 n) {
        Vertex out = this.copy();
        out.setNormal(n);
        return out;
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    /** Compute a triangle face normal (right-handed, counter-clockwise winding). Returns a NEW normalized Vector3. */
    public static Vector3 faceNormal(Vertex a, Vertex b, Vertex c) {
        // n = normalize( (b-a) x (c-a) )
        float ax = a.pos.x, ay = a.pos.y, az = a.pos.z;
        float bx = b.pos.x, by = b.pos.y, bz = b.pos.z;
        float cx = c.pos.x, cy = c.pos.y, cz = c.pos.z;

        float abx = bx - ax, aby = by - ay, abz = bz - az;
        float acx = cx - ax, acy = cy - ay, acz = cz - az;

        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;

        float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 1e-12f) { nx /= len; ny /= len; nz /= len; } else { nx = ny = nz = 0f; }
        return new Vector3(nx, ny, nz);
    }

    /**
     * Barycentric interpolation between three vertices (positions, uvs, normals if present).
     * Useful for perspective-correct attribute interpolation after dividing by w (you handle the 1/w weighting outside).
     */
    public static Vertex lerp(Vertex a, Vertex b, Vertex c, float wa, float wb, float wc) {
        // Position
        float px = a.pos.x * wa + b.pos.x * wb + c.pos.x * wc;
        float py = a.pos.y * wa + b.pos.y * wb + c.pos.y * wc;
        float pz = a.pos.z * wa + b.pos.z * wb + c.pos.z * wc;

        // UV
        float tu = a.tex.x * wa + b.tex.x * wb + c.tex.x * wc;
        float tv = a.tex.y * wa + b.tex.y * wb + c.tex.y * wc;

        Vertex out = new Vertex(px, py, pz, tu, tv);

        // Normal (only if any provided)
        boolean an = a.norm != null, bn = b.norm != null, cn = c.norm != null;
        if (an || bn || cn) {
            float nx = (an ? a.norm.x : 0f) * wa + (bn ? b.norm.x : 0f) * wb + (cn ? c.norm.x : 0f) * wc;
            float ny = (an ? a.norm.y : 0f) * wa + (bn ? b.norm.y : 0f) * wb + (cn ? c.norm.y : 0f) * wc;
            float nz = (an ? a.norm.z : 0f) * wa + (bn ? b.norm.z : 0f) * wb + (cn ? c.norm.z : 0f) * wc;
            out.norm = new Vector3(nx, ny, nz).normalize();
        }
        return out;
    }

    /*
     * Optional transform hooks — wire these to YOUR math types.
     * Use modelMatrix for positions; use inverse-transpose(modelMatrix).upper3x3 for normals.
     *
     * Example (pseudo-API you likely have):
     *   pos.set(model.mulPoint(pos)); // applies translation/scale/rotation
     *   norm.set(normalMat.mulVector(norm)).normalize(); // no translation; keep length=1
     */
    public Vertex transformPosition(Matrix4 model) {
        // TODO: replace with your actual multiply, e.g., pos.set(model.mulPoint(pos));
        // fallback naive (if you have a method already, use it instead)
        return this;
    }

    public Vertex transformNormal(Matrix3 normalMatrix) {
        // TODO: replace with normal transform (upper 3x3 of inverse-transpose(model))
        return this;
    }
    
}
