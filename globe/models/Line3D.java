package controller.globe.models;

import java.awt.Color;

import controller.globe.math.Vector3;
import eutil.colors.EColors;
import eutil.datatypes.boxes.Box2;

// Line drawing algorithms: http://members.chello.at/easyfilter/bresenham.html

public class Line3D extends Model {
    
    //========
    // Fields
    //========
    
    public int lineWidth = 1;
    // not implemented
    public boolean antiAlias = false;
    // not implemented
    public boolean drawDots;
    public boolean drawLine = true;
    
    //==============
    // Constructors
    //==============
    
    public Line3D() { this(EColors.white); }
    public Line3D(Color lineColor) {
        this.color = lineColor;
    }
    
    public Line3D(Vector3... points) { this(EColors.white, points); }
    public Line3D(Color color, Vector3... points) {
        this.color = color;
        this.points.addA(points);
    }    
    //===========
    // Overrides
    //===========
    
    @Override
    public Line3D copy() {
        Line3D c = new Line3D(color);
        c.points.addAll(points);
        c.antiAlias = antiAlias;
        c.drawDots = drawDots;
        c.drawLine = drawLine;
        return null;
    }
    
    //=========
    // Methods
    //=========
    
    public void addPoint(Vector3 point) { addPoint(point.x, point.y, point.z); }
    public void addPoint(float x, float y, float z) {
        points.add(new Vector3(x, y, z));
    }
    
    public void addPoints(Vector3... points) {
        for (var p : points) {
            this.points.add(p);
        }
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    public static Box2<Vector3, Vector3> clipLineAgainstPlane(Vector3 plane, Vector3 normal, Vector3 start, Vector3 end) {
        // make sure plane is normal
        normal = normal.normalize();
        // return signed shortest distance from point to plane, plane normal must be normalized
        float dotNP = normal.dot(plane);
        float d0 = (normal.x * start.x + normal.y * start.y + normal.z * start.z - dotNP);
        float d1 = (normal.x * end.x   + normal.y * end.y   + normal.z * end.z   - dotNP);
        
        Vector3[] vin = new Vector3[2];
        Vector3[] vout = new Vector3[2];
        int vInCount = 0, vOutCount = 0;
        
        if (d0 >= 0) vin[vInCount++] = start; else vout[vOutCount++] = start;
        if (d1 >= 0) vin[vInCount++] = end;   else vout[vOutCount++] = end;
        
        // if only one point is in the plane, clip the line to the point where it intersects with the plane
        if (vInCount == 1) return new Box2<>(vin[0], Vector3.intersectPlane(plane, normal, vin[0], vout[0]));
        // if both points are inside the plane, don't clip anything and just return the points as they are
        if (vInCount == 2) return new Box2<>(start, end);
        // if neither point is inside of the plane, clip both points and return nothing
        return null;
    }
    
}
