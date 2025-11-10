package controller.globe.util;

import controller.globe.models.Triangle;
import eutil.datatypes.util.EList;

public class DrawBatch {
    
    //========
    // Fields
    //========
    
    public final Material material;           // may be null
    public final Texture diffuseTexture;      // may be null
    public final EList<Triangle> triangles;   // triangles in this batch
    
    //==============
    // Constructors
    //==============
    
    public DrawBatch(Material m, Texture t, EList<Triangle> tris) {
        material = m; diffuseTexture = t; triangles = tris;
    }
}
