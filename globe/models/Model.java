package controller.globe.models;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import controller.globe.math.Vector2;
import controller.globe.math.Vector3;
import controller.globe.math.Vertex;
import controller.globe.util.DrawBatch;
import controller.globe.util.Material;
import controller.globe.util.Texture;
import eutil.colors.EColors;
import eutil.datatypes.util.EList;
import eutil.file.EFileUtil;
import eutil.file.LineReader;
import eutil.math.ENumUtil;

public abstract class Model {
    
    //========
    // Fields
    //========
    
    public Color color;
    public BufferedImage texture;
    
    public EList<Vector3> points = EList.newList();
    public EList<Triangle> triangles = EList.newList();
    
    public final EList<Material> materials = EList.newList();
    public final EList<Texture> textures = EList.newList();
    public final EList<Integer> triMaterial = EList.newList();
    
    public Map<String, Integer> materialIndex = new HashMap<>();
    public Map<String, Integer> textureIndexByPath = new HashMap<>();
    
    public EList<Model> childModels = EList.newList();
    
    public boolean drawFilled = true;
    public boolean insideOut = false;
    public boolean fullBright = false;
    public boolean visible = true;
    
    public float opacity = 1.0f;
    public float minimumBrightness = 0.0f;
    
    //===========
    // Abstracts
    //===========
    
    public abstract Model copy();
    
    //=========
    // Methods
    //=========
    
    public boolean isTransparent() {
        return opacity < 1.0f || (color != null && color.getAlpha() < 255);
    }
    
    //=========
    // Setters
    //=========
    
    public void setTexture(BufferedImage tex) {
        if (tex == null) return;
        color = EColors.red;
        texture = tex;
        triangles.forEach(t -> t.setTexture(tex));
    }
    
    public void setColor(Color color) {
        if (color == null) return;
        this.color = color;
        texture = null;
        triangles.forEach(t -> t.color = color);
    }
    
    public void setOpacity(float val) {
        if (Float.isNaN(val)) val = 0.0f;
        val = ENumUtil.clamp(val, 0.0f, 1.0f);
        this.opacity = val;
        int opacityInt = (int) (255f * val);
        
        for (int i = 0; i < triangles.size(); i++) {
            int rgb = triangles.get(i).color.getRGB();
            int argb = EColors.changeOpacity(rgb, opacityInt);
            triangles.get(i).color = new Color(argb, true);
        }
    }
    
    /** Ensure triangle->material mapping stays in sync with triangles list size. */
    public void syncTriMaterial() {
        while (triMaterial.size() < triangles.size()) triMaterial.add(-1);
        while (triangles.size() < triMaterial.size()) triMaterial.remove(triMaterial.size() - 1);
    }
    
    /** Adds or returns existing material index by name. */
    public int ensureMaterial(String name) {
        if (name == null) return -1;
        Integer idx = materialIndex.get(name);
        if (idx != null) return idx;
        Material m = new Material(name);
        materials.add(m);
        int i = materials.size() - 1;
        materialIndex.put(name, i);
        return i;
    }
    
    /** Adds/returns texture index by normalized path (lowercased, forward slashes). */
    public int ensureTexture(String normalizedPath, BufferedImage image) {
        if (normalizedPath == null) return -1;
        Integer idx = textureIndexByPath.get(normalizedPath);
        if (idx != null) return idx;
        textures.add(new Texture(normalizedPath, image));
        int i = textures.size() - 1;
        textureIndexByPath.put(normalizedPath, i);
        return i;
    }
    
    /** Convenience: build render batches by material to reduce binds. */
    public EList<DrawBatch> buildBatches() {
        EList<DrawBatch> out = EList.newList();
        Map<Integer, EList<Triangle>> map = new LinkedHashMap<>();
        
        for (int i = 0; i < triangles.size(); i++) {
            int m = (i < triMaterial.size()) ? triMaterial.get(i) : -1;
            map.computeIfAbsent(m, k -> EList.newList()).add(triangles.get(i));
        }
        
        for (var e : map.entrySet()) {
            int mi = e.getKey();
            Material mat = (mi >= 0 && mi < materials.size()) ? materials.get(mi) : null;
            Texture tex = null;
            if (mat != null && mat.mapKdTexIndex >= 0 && mat.mapKdTexIndex < textures.size()) {
                tex = textures.get(mat.mapKdTexIndex);
            }
            out.add(new DrawBatch(mat, tex, e.getValue()));
        }
        
        return out;
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
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
        try (var r = new LineReader(file)) {
            EList<Triangle> loaded = EList.newList();
            EList<Vector3> verts = EList.newList();
            
            while (r.hasNextLine()) {
                String line = r.nextLine();
                String[] parts = line.split(" ");
                
                // comments
                if (parts.length == 0 || parts[0].equals("#")) continue;
                
                if (parts.length == 4) {
                    // vertex
                    if (parts[0].equals("v")) {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        verts.add(new Vector3(x, y, z));
                    }
                    if (parts[0].equals("f")) {
                        int f0 = Integer.parseInt(parts[1]);
                        int f1 = Integer.parseInt(parts[2]);
                        int f2 = Integer.parseInt(parts[3]);
                        Vertex v0 = new Vertex(verts.get(f0 - 1), new Vector2());
                        Vertex v1 = new Vertex(verts.get(f1 - 1), new Vector2());
                        Vertex v2 = new Vertex(verts.get(f2 - 1), new Vector2());
                        loaded.add(new Triangle(v0, v1, v2, Color.WHITE));
                    }
                }
            }
            
            Model model = new Model() {
                @Override public Model copy() {
                    return this;
                }
            };
            model.triangles.addAll(loaded);
            return model;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
}
