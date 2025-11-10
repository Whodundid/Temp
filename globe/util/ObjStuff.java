package controller.globe.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import controller.globe.math.Vector2;
import controller.globe.math.Vector3;
import controller.globe.math.Vertex;

public class ObjStuff {
    
    static final class FaceRef {
        int v = Integer.MIN_VALUE, vt = Integer.MIN_VALUE, vn = Integer.MIN_VALUE;
    }
    
    static final class Face {
        String materialName;
        List<FaceRef> refs = new ArrayList<>();
    }
    
    static final class ObjData {
        List<Vector3> pos = new ArrayList<>();
        List<Vector2> uv = new ArrayList<>();
        List<Vector3> nor = new ArrayList<>();
        List<String> mtlFiles = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
    }
    
    static ObjData parseObj(String text, boolean flipV) throws IOException {
        ObjData d = new ObjData();
        String currentMtl = null;
        try (var br = new BufferedReader(new StringReader(text))) {
            for (String raw; (raw = br.readLine()) != null;) {
                int h = raw.indexOf('#');
                String line = (h >= 0 ? raw.substring(0, h) : raw).trim();
                if (line.isEmpty()) continue;
                String[] tok = line.split("\\s+");
                switch (tok[0]) {
                case "v":
                    if (tok.length >= 4) d.pos.add(new Vector3(Float.parseFloat(tok[1]), Float.parseFloat(tok[2]), Float
                        .parseFloat(tok[3])));
                    break;
                case "vt":
                    if (tok.length >= 3) {
                        float u = Float.parseFloat(tok[1]), v = Float.parseFloat(tok[2]);
                        if (flipV) v = 1f - v;
                        d.uv.add(new Vector2(u, v));
                    }
                    break;
                case "vn":
                    if (tok.length >= 4) d.nor.add(new Vector3(Float.parseFloat(tok[1]), Float.parseFloat(tok[2]), Float
                        .parseFloat(tok[3])));
                    break;
                case "usemtl":
                    currentMtl = (tok.length >= 2) ? tok[1] : null;
                    break;
                case "mtllib":
                    for (int i = 1; i < tok.length; i++)
                        d.mtlFiles.add(tok[i]);
                    break;
                case "f":
                    if (tok.length < 4) break;
                    Face f = new Face();
                    f.materialName = currentMtl;
                    for (int i = 1; i < tok.length; i++)
                        f.refs.add(parseRef(tok[i]));
                    d.faces.add(f);
                    break;
                }
            }
        }
        return d;
    }
    
    static FaceRef parseRef(String token) {
        FaceRef r = new FaceRef();
        int s1 = token.indexOf('/');
        if (s1 < 0) {
            r.v = parseI(token);
            return r;
        }
        int s2 = token.indexOf('/', s1 + 1);
        r.v = parseI(token.substring(0, s1));
        if (s2 < 0) {
            r.vt = parseI(safeSub(token, s1 + 1));
        }
        else {
            String mid = token.substring(s1 + 1, s2);
            r.vt = mid.isEmpty() ? Integer.MIN_VALUE : parseI(mid);
            r.vn = parseI(safeSub(token, s2 + 1));
        }
        return r;
    }
    
    static String safeSub(String s, int start) {
        return start < s.length() ? s.substring(start) : "";
    }
    
    static int parseI(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }
    
    static int resolve(int idxObj, int size) {
        if (idxObj == Integer.MIN_VALUE) return 0;
        int idx = (idxObj > 0) ? (idxObj - 1) : (size + idxObj);
        if (idx < 0) idx = 0;
        if (idx >= size) idx = size - 1;
        return idx;
    }
    
    static Vertex makeVertex(ObjData d, FaceRef ref) {
        int pi = resolve(ref.v, d.pos.size());
        int ti = (ref.vt == Integer.MIN_VALUE) ? Integer.MIN_VALUE : resolve(ref.vt, d.uv.size());
        int ni = (ref.vn == Integer.MIN_VALUE) ? Integer.MIN_VALUE : resolve(ref.vn, d.nor.size());
        Vector3 p = d.pos.get(pi);
        Vector2 uv = (ti != Integer.MIN_VALUE) ? d.uv.get(ti) : new Vector2(0f, 0f);
        Vertex v = new Vertex(p, uv);
        if (ni != Integer.MIN_VALUE) v.setNormal(d.nor.get(ni));
        return v;
    }
    
}
