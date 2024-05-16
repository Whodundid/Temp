package controller.globe;

import eutil.datatypes.points.Point3f;
import eutil.datatypes.util.EList;

public abstract class Shape {
    
    public EList<Point3f> points = EList.newList();
    public EList<Triangle> triangles = EList.newList();
    public Vector position = new Vector();
    public Vector rotation = new Vector();
    public Vector scale = new Vector(1, 1, 1);
    
    protected Shape() {}
    protected Shape(Vector posIn) { this(posIn.x, posIn.y, posIn.z); }
    protected Shape(float x, float y, float z) {
        position.set(x, y, z);;
    }
    
    public void setRotation(float x, float y, float z) {
        rotation.set((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }
    
}
