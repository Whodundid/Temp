package controller.globe;

import eutil.datatypes.points.Point3f;
import eutil.datatypes.util.EList;

public class Model {
    
    public boolean insideOut = false;
    public boolean fullBright = false;
    public EList<Point3f> points = EList.newList();
    public EList<Triangle> triangles = EList.newList();
    
}
