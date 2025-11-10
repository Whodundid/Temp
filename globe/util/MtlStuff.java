package controller.globe.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class MtlStuff {
    
    static final class MtlMat {
        String name;
        float Ns = 0f, d = 1f;
        int illum = 2;
        float[] Ka = { 0, 0, 0 }, Kd = { 1, 1, 1 }, Ks = { 0, 0, 0 };
        String map_Kd, map_Ks, map_d, map_bump;
    }
    
    static Map<String, MtlMat> parseMtl(String text) throws IOException {
        var out = new LinkedHashMap<String, MtlMat>();
        MtlMat cur = null;
        try (var br = new BufferedReader(new StringReader(text))) {
            for (String raw; (raw = br.readLine()) != null;) {
                int h = raw.indexOf('#');
                String line = (h >= 0 ? raw.substring(0, h) : raw).trim();
                if (line.isEmpty()) continue;
                String[] tok = line.split("\\s+");
                switch (tok[0]) {
                case "newmtl":
                    cur = new MtlMat();
                    cur.name = tok[1];
                    out.put(cur.name, cur);
                    break;
                case "Ka":
                    if (cur != null && tok.length >= 4) {
                        cur.Ka = new float[] { f(tok[1]), f(tok[2]), f(tok[3]) };
                    }
                    break;
                case "Kd":
                    if (cur != null && tok.length >= 4) {
                        cur.Kd = new float[] { f(tok[1]), f(tok[2]), f(tok[3]) };
                    }
                    break;
                case "Ks":
                    if (cur != null && tok.length >= 4) {
                        cur.Ks = new float[] { f(tok[1]), f(tok[2]), f(tok[3]) };
                    }
                    break;
                case "Ns":
                    if (cur != null && tok.length >= 2) {
                        cur.Ns = f(tok[1]);
                    }
                    break;
                case "d":
                    if (cur != null && tok.length >= 2) {
                        cur.d = f(tok[1]);
                    }
                    break;
                case "Tr":
                    if (cur != null && tok.length >= 2) {
                        cur.d = 1f - f(tok[1]);
                    }
                    break;
                case "illum":
                    if (cur != null && tok.length >= 2) {
                        cur.illum = (int) f(tok[1]);
                    }
                    break;
                case "map_Kd":
                    if (cur != null && tok.length >= 2) {
                        cur.map_Kd = joinFrom(tok, 1);
                    }
                    break;
                case "map_Ks":
                    if (cur != null && tok.length >= 2) {
                        cur.map_Ks = joinFrom(tok, 1);
                    }
                    break;
                case "map_d":
                    if (cur != null && tok.length >= 2) {
                        cur.map_d = joinFrom(tok, 1);
                    }
                    break;
                case "map_Bump":
                case "bump":
                    if (cur != null && tok.length >= 2) {
                        cur.map_bump = joinFrom(tok, 1);
                    }
                    break;
                }
            }
        }
        return out;
    }
    
    static float f(String s) {
        return Float.parseFloat(s);
    }
    
    static String joinFrom(String[] t, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < t.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(t[i]);
        }
        return sb.toString();
    }
    
}
