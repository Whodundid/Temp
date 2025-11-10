package controller.globe.util;

public class Material {
    
    //========
    // Fields
    //========
    
    public String name;
    public float Ns = 0f; // spec exponent
    public float d = 1f; // opacity (or 1 - Tr)
    public int illum = 2;
    
    public float Ka_r = 0, Ka_g = 0, Ka_b = 0; // ambient
    public float Kd_r = 1, Kd_g = 1, Kd_b = 1; // diffuse
    public float Ks_r = 0, Ks_g = 0, Ks_b = 0; // specular
    
    // indices into Model.textures (or -1 if none)
    public int mapKdTexIndex = -1; // diffuse/albedo
    public int mapKsTexIndex = -1; // specular
    public int mapDTexIndex = -1; // opacity
    public int mapBumpTexIndex = -1; // normal/bump
    
    //==============
    // Constructors
    //==============
    
    public Material() {}
    public Material(String name) {
        this.name = name;
    }
    
}
