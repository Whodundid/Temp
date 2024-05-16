package controller.globe;

import java.awt.Color;

public class Sphere extends Shape {
    
    //==============
    // Constructors
    //==============
    
    public Sphere() { this(0.0f, 0.0f, 0.0f, 1.0f); }
    public Sphere(float radius) { this(0.0f, 0.0f, 0.0f, radius); }
    public Sphere(float x, float y, float z, float radius) {
        super(x, y, z);
        setup(radius);
    }
    
    //=========
    // Methods
    //=========
    
    public void setup(float radius) {
        float stackCount = 18;
        float sectorCount = 36;
        
        float PI = (float) Math.PI;
        float sectorStep = 2 * PI / sectorCount;
        float stackStep = PI / stackCount;
        
        // mid
        {
            for (int i = 0; i < stackCount; i++) {
                float stackAngleTop = PI / 2 - i * stackStep;
                float stackAngleBot = PI / 2 - (i + 1) * stackStep;
                float zTop = (float) (radius * Math.sin(stackAngleTop));
                float zBot = (float) (radius * Math.sin(stackAngleBot));
                
                for (int j = 0; j < sectorCount; j++) {
                    float sectorAngle1 = j * sectorStep;
                    float sectorAngle2 = (j + 1) * sectorStep;
                    float x1 = (float) (radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle1));
                    float y1 = (float) (radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle1));
                    float x2 = (float) (radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle2));
                    float y2 = (float) (radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle2));
                    float x3 = (float) (radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle1));
                    float y3 = (float) (radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle1));
                    float x4 = (float) (radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle2));
                    float y4 = (float) (radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle2));
                    
                    Vector v1 = new Vector(x1, y1, zTop);
                    Vector v2 = new Vector(x2, y2, zTop);
                    Vector v3 = new Vector(x3, y3, zBot);
                    Vector v4 = new Vector(x4, y4, zBot);
                    
                    triangles.add(new Triangle(v1, v3, v2, Color.WHITE));
                    triangles.add(new Triangle(v2, v3, v4, Color.GRAY));
                }
            }
        }
        
//        for (int i = 0; i <= stackCount; i++) {
//            stackAngle = PI / 2 - i * stackStep;        // starting from pi/2 to -pi/2
//            xy = radius * cosf(stackAngle);             // r * cos(u)
//            z = radius * sinf(stackAngle);              // r * sin(u)
//
//            // add (sectorCount+1) vertices per stack
//            // first and last vertices have same position and normal, but different tex coords
//            for(int j = 0; j <= sectorCount; ++j)
//            {
//                sectorAngle = j * sectorStep;           // starting from 0 to 2pi
//
//                // vertex position (x, y, z)
//                x = xy * cosf(sectorAngle);             // r * cos(u) * cos(v)
//                y = xy * sinf(sectorAngle);             // r * cos(u) * sin(v)
//                vertices.push_back(x);
//                vertices.push_back(y);
//                vertices.push_back(z);
//
//                // normalized vertex normal (nx, ny, nz)
//                nx = x * lengthInv;
//                ny = y * lengthInv;
//                nz = z * lengthInv;
//                normals.push_back(nx);
//                normals.push_back(ny);
//                normals.push_back(nz);
//
//                // vertex tex coord (s, t) range between [0, 1]
//                s = (float)j / sectorCount;
//                t = (float)i / stackCount;
//                texCoords.push_back(s);
//                texCoords.push_back(t);
//            }
//        }
    }
    
}
