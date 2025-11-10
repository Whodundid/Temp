package controller.globe.util;

import java.awt.Color;
import java.io.File;

import controller.globe.math.Vector2;
import controller.globe.math.Vector3;
import controller.globe.math.Vertex;
import controller.globe.models.Model;
import controller.globe.models.Triangle;
import eutil.datatypes.util.EList;
import eutil.file.EFileUtil;
import eutil.file.LineReader;

public class ObjLoader {
    
    public static Model loadModelFromObjFile(String fileName) {
        try {
            var loader = Thread.currentThread().getContextClassLoader();
            File file = new File(loader.getResource(fileName).toURI());
            return loadModelFromObjFile(file);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Model loadModelFromObjFile(File file) {
        if (EFileUtil.notExists(file)) return null;
        
        // Raw attribute pools
        EList<Vector3> positions = EList.newList();
        EList<Vector2> uvs = EList.newList();
        EList<Vector3> normals = EList.newList();
        
        EList<Triangle> triangles = EList.newList();
        
        // Track current material color if you want to map it later; default white
        Color currentColor = Color.WHITE;
        
        try (var r = new LineReader(file)) {
            while (r.hasNextLine()) {
                String line = r.nextLine();
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                // Split on any whitespace (multiple spaces / tabs safe)
                String[] tok = line.split("\\s+");
                String head = tok[0];
                
                switch (head) {
                case "v":
                { // vertex position
                    // v x y z [w]
                    if (tok.length < 4) break;
                    float x = parseF(tok[1]), y = parseF(tok[2]), z = parseF(tok[3]);
                    positions.add(new Vector3(x, y, z));
                    break;
                }
                case "vt":
                { // texture coord
                    // vt u v [w]
                    if (tok.length < 3) break;
                    float u = parseF(tok[1]), v = parseF(tok[2]);
                    uvs.add(new Vector2(u, v));
                    break;
                }
                case "vn":
                { // normal
                    if (tok.length < 4) break;
                    float x = parseF(tok[1]), y = parseF(tok[2]), z = parseF(tok[3]);
                    normals.add(new Vector3(x, y, z));
                    break;
                }
                case "f":
                { // face (triangle, quad, polygon)
                    // tokens like: f v1 v2 v3 ...
                    // or f v1/vt1 v2/vt2 v3/vt3 ...
                    // or f v1//vn1 v2//vn2 v3//vn3 ...
                    // or f v1/vt1/vn1 ...
                    if (tok.length < 4) break; // need at least a triangle
                    
                    // Parse each face vertex to triplets of (posIdx, uvIdx, normIdx)
                    int n = tok.length - 1;
                    int[] pIdx = new int[n];
                    int[] tIdx = new int[n];
                    int[] nIdx = new int[n];
                    
                    for (int i = 0; i < n; i++) {
                        FaceRef ref = parseFaceRef(tok[i + 1]);
                        // Convert OBJ 1-based (and negative) to 0-based arrays
                        pIdx[i] = resolveIndex(ref.v, positions.size());
                        tIdx[i] = (ref.vt != Integer.MIN_VALUE) ? resolveIndex(ref.vt, uvs.size()) : Integer.MIN_VALUE;
                        nIdx[i] = (ref.vn != Integer.MIN_VALUE) ? resolveIndex(ref.vn, normals.size())
                                                                : Integer.MIN_VALUE;
                    }
                    
                    // Triangulate by fan: (0, i, i+1)
                    for (int i = 1; i < n - 1; i++) {
                        Vertex a = makeVertex(positions, uvs, normals, pIdx[0], tIdx[0], nIdx[0]);
                        Vertex b = makeVertex(positions, uvs, normals, pIdx[i], tIdx[i], nIdx[i]);
                        Vertex c = makeVertex(positions, uvs, normals, pIdx[i + 1], tIdx[i + 1], nIdx[i + 1]);
                        triangles.add(new Triangle(a, b, c, currentColor));
                    }
                    break;
                }
                case "usemtl":
                {
                    // If you have an MTL to Color map, set currentColor here
                    // Example stub: currentColor = MaterialLib.colorFor(tok.length > 1 ? tok[1] : "default");
                    break;
                }
                case "mtllib":
                case "o":
                case "g":
                case "s":
                    // Ignored for now
                    break;
                
                default:
                    // Unknown line type; ignore politely
                    break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        // Wrap into your Model
        Model model = new Model() {
            @Override
            public Model copy() {
                return this;
            }
        };
        model.triangles.addAll(triangles);
        return model;
    }
    
    // ---- helpers ----
    
    private static float parseF(String s) {
        // tolerant float parse (avoids NFE noise elsewhere)
        return Float.parseFloat(s);
    }
    
    private static int resolveIndex(int objIndex, int size) {
        // OBJ indices are 1-based; negatives are relative to the end (-1 = last)
        if (objIndex == Integer.MIN_VALUE) return Integer.MIN_VALUE;
        int idx = (objIndex > 0) ? (objIndex - 1) : (size + objIndex);
        // Clamp (defensive)
        if (idx < 0) idx = 0;
        if (idx >= size) idx = size - 1;
        return idx;
    }
    
    private static Vertex makeVertex(EList<Vector3> positions, EList<Vector2> uvs, EList<Vector3> normals, int pi, int ti, int ni) {
        // Position (required)
        Vector3 pos = positions.get(pi);
        
        // UV (optional)
        Vector2 uv;
        if (ti != Integer.MIN_VALUE && ti >= 0 && ti < uvs.size()) {
            uv = uvs.get(ti);
        }
        else {
            uv = new Vector2(0f, 0f);
        }
        
        // Build vertex
        Vertex v = new Vertex(pos, uv);
        
        // Normal (optional)
        if (ni != Integer.MIN_VALUE && ni >= 0 && ni < normals.size()) {
            v.setNormal(normals.get(ni));
        }
        
        return v;
    }

    
    private static final class FaceRef {
        final int v, vt, vn;
        
        FaceRef(int v, int vt, int vn) {
            this.v = v;
            this.vt = vt;
            this.vn = vn;
        }
    }
    
    /**
     * Parses a single face vertex reference like: "7", "7/3", "7//2", "7/3/2",
     * "-1/-1/-1" Returns indices as given in the file (1-based positive,
     * negative for relative), or Integer.MIN_VALUE if a field is absent.
     */
    private static FaceRef parseFaceRef(String token) {
        // Fast path: no slash -> only position
        int slash1 = token.indexOf('/');
        if (slash1 < 0) {
            return new FaceRef(parseIntSafe(token), Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        
        int slash2 = token.indexOf('/', slash1 + 1);
        if (slash2 < 0) {
            // v/vt
            int v = parseIntSafe(token.substring(0, slash1));
            int vt = parseIntSafe(token.substring(slash1 + 1));
            return new FaceRef(v, vt, Integer.MIN_VALUE);
        }
        else {
            // v//vn  or  v/vt/vn (vt may be empty)
            int v = parseIntSafe(token.substring(0, slash1));
            String mid = token.substring(slash1 + 1, slash2);
            int vt = mid.isEmpty() ? Integer.MIN_VALUE : parseIntSafe(mid);
            int vn = parseIntSafe(token.substring(slash2 + 1));
            return new FaceRef(v, vt, vn);
        }
    }
    
    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }
    
}
