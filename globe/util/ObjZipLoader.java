package controller.globe.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import controller.globe.math.Vertex;
import controller.globe.models.Model;
import controller.globe.models.Triangle;

public class ObjZipLoader {
    
    public static Model loadModelFromZip(String fileName) { return loadModelFromZip(fileName, null, true); }
    public static Model loadModelFromZip(String fileName, boolean flipV) { return loadModelFromZip(fileName, null, flipV); }
    public static Model loadModelFromZip(String fileName, String objFileName, boolean flipV) {
        try {
            var loader = Thread.currentThread().getContextClassLoader();
            File file = new File(loader.getResource(fileName).toURI());
            return loadModelFromZip(file, objFileName, flipV);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Model loadModelFromZip(File zipFile, String objPathOrNull, boolean flipV) {
        try {
            // Read zip to memory
            var zipMap = ZipStuff.readZipToMemory(zipFile);
            
            // Pick OBJ
            String objKey = (objPathOrNull != null) ? ZipStuff.normalizePath(objPathOrNull) : ZipStuff.findFirstWithExtension(zipMap.keySet(), ".obj");
            if (objKey == null) throw new RuntimeException("No .obj in zip");
            
            // Parse OBJ (positions, uvs, normals, faces, mtllib/usemtl)
            var objData = ObjStuff.parseObj(new String(zipMap.get(objKey), StandardCharsets.UTF_8), flipV);
            
            // Create model
            Model model = new Model() {
                @Override
                public Model copy() {
                    return this;
                }
            };
            
            // Parse MTLs and load textures
            String objDir = ZipStuff.parentDir(objKey);
            Map<String, Material> mtlByName = new LinkedHashMap<>();
            
            for (String mtlRel : objData.mtlFiles) {
                String mtlKey = ZipStuff.resolveZipPath(objDir, mtlRel);
                
                byte[] mtlBytes = zipMap.get(mtlKey);
                if (mtlBytes == null) continue;
                
                var parsed = MtlStuff.parseMtl(new String(mtlBytes, StandardCharsets.UTF_8));
                // register materials + their textures into model
                String mtlDir = ZipStuff.parentDir(mtlKey);
                for (var m : parsed.values()) {
                    int midx = model.ensureMaterial(m.name);
                    Material mm = model.materials.get(midx);
                    
                    // copy scalars
                    mm.Ns = m.Ns;
                    mm.d = m.d;
                    mm.illum = m.illum;
                    mm.Ka_r = m.Ka[0];
                    mm.Ka_g = m.Ka[1];
                    mm.Ka_b = m.Ka[2];
                    mm.Kd_r = m.Kd[0];
                    mm.Kd_g = m.Kd[1];
                    mm.Kd_b = m.Kd[2];
                    mm.Ks_r = m.Ks[0];
                    mm.Ks_g = m.Ks[1];
                    mm.Ks_b = m.Ks[2];
                    
                    // textures
                    mm.mapKdTexIndex = ZipStuff.ensureTextureFromMap(model, zipMap, mtlDir, m.map_Kd);
                    mm.mapKsTexIndex = ZipStuff.ensureTextureFromMap(model, zipMap, mtlDir, m.map_Ks);
                    mm.mapDTexIndex = ZipStuff.ensureTextureFromMap(model, zipMap, mtlDir, m.map_d);
                    mm.mapBumpTexIndex = ZipStuff.ensureTextureFromMap(model, zipMap, mtlDir, m.map_bump);
                    
                    mtlByName.put(m.name, mm);
                }
            }
            
            // Build triangles & tag materials
            for (var face : objData.faces) {
                int n = face.refs.size();
                if (n < 3) continue;
                
                int matIndex = (face.materialName != null) ? model.ensureMaterial(face.materialName) : -1;
                Material material = model.materials.get(matIndex);
                
                Vertex v0 = ObjStuff.makeVertex(objData, face.refs.get(0));
                for (int i = 1; i < n - 1; i++) {
                    Vertex v1 = ObjStuff.makeVertex(objData, face.refs.get(i));
                    Vertex v2 = ObjStuff.makeVertex(objData, face.refs.get(i + 1));
                    // after you’ve parsed materials and loaded textures from the MTL:
                    Triangle t = new Triangle(v0, v1, v2, Color.WHITE);

                    // Attach diffuse texture if the active material has one
                    BufferedImage diffuse = null;
                    if (material != null && material.mapKdTexIndex >= 0 && material.mapKdTexIndex < model.textures.size()) {
                        diffuse = model.textures.get(material.mapKdTexIndex).image; // or however you store it
                    }
                    t.texture = diffuse;
                    
                    // Also store Kd as the triangle color for modulation later
                    if (material != null) {
                        int r = Math.round(material.Kd_r * 255f);
                        int g = Math.round(material.Kd_g * 255f);
                        int b = Math.round(material.Kd_b * 255f);
                        int a = Math.round(material.d * 255f); // opacity if you care; otherwise 255
                        t.color = new Color(r, g, b, a);
                    }
                    else {
                        t.color = Color.WHITE;
                    }

                    model.triangles.add(t);
                    // set tri material
                    model.triMaterial.add(matIndex);
                }
            }
            model.syncTriMaterial();
            return model;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
}
