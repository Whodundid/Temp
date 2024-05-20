package controller.globe;

import java.awt.Color;

import eutil.datatypes.util.EList;

public class Line3D extends Model {
    
    //========
    // Fields
    //========
    
    public Color lineColor = Color.WHITE;
    public final EList<Vector3> lineSegments = EList.newList();
    
    //==============
    // Constructors
    //==============
    
    public Line3D() { this(Color.WHITE); }
    public Line3D(Color lineColor) {
        this.lineColor = lineColor;
    }
    
    //=========
    // Methods
    //=========
    
    public void addPoint(Vector3 point) { addPoint(point.x, point.y, point.z); }
    public void addPoint(float x, float y, float z) {
        Vector3 p = new Vector3(x, y, z);
        lineSegments.add(p);
        points.add(p);
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    public static Vector3 intersectPlane(Vector3 p, Vector3 n, Vector3 lineStart, Vector3 lineEnd) {
        return null;
    }
    
}
