package controller.globe;

import eutil.datatypes.util.EList;

public class Model {
    
    public boolean insideOut = false;
    public boolean fullBright = false;
    public EList<Vector3> points = EList.newList();
    public EList<Triangle> triangles = EList.newList();
    
}
