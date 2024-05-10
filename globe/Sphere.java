package controller.globe;

import java.awt.Color;

import eutil.datatypes.util.EList;

public class Sphere {
    
    //========
    // Fields
    //========
    
    public EList<Triangle> triangles = EList.newList();
    
    //==============
    // Constructors
    //==============
    
    public Sphere() {
        setup();
    }
    
    //=========
    // Methods
    //=========
    
    public void setup() {
        
        double radius = 100;
        
        double stackCount = 18;
        double sectorCount = 36;
        
        double PI = Math.PI;
        double sectorStep = 2 * PI / sectorCount;
        double stackStep = PI / stackCount;
        
        // mid
        {
            for (int i = 0; i < stackCount; i++) {
                double stackAngleTop = PI / 2 - i * stackStep;
                double stackAngleBot = PI / 2 - (i + 1) * stackStep;
                double zTop = radius * Math.sin(stackAngleTop);
                double zBot = radius * Math.sin(stackAngleBot);
                
                for (int j = 0; j < sectorCount; j++) {
                    double sectorAngle1 = j * sectorStep;
                    double sectorAngle2 = (j + 1) * sectorStep;
                    double x1 = radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle1);
                    double y1 = radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle1);
                    double x2 = radius * Math.cos(stackAngleTop) * Math.cos(sectorAngle2);
                    double y2 = radius * Math.cos(stackAngleTop) * Math.sin(sectorAngle2);
                    double x3 = radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle1);
                    double y3 = radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle1);
                    double x4 = radius * Math.cos(stackAngleBot) * Math.cos(sectorAngle2);
                    double y4 = radius * Math.cos(stackAngleBot) * Math.sin(sectorAngle2);
                    
                    Vertex v1 = new Vertex(x1, y1, zTop);
                    Vertex v2 = new Vertex(x2, y2, zTop);
                    Vertex v3 = new Vertex(x3, y3, zBot);
                    Vertex v4 = new Vertex(x4, y4, zBot);
                    
                    triangles.add(new Triangle(v1, v3, v2, Color.GREEN));
                    triangles.add(new Triangle(v2, v3, v4, Color.RED));
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
