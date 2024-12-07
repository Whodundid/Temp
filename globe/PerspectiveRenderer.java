package controller.globe;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import eutil.colors.EColors;
import eutil.datatypes.boxes.Box2;
import eutil.datatypes.boxes.BoxList;
import eutil.datatypes.util.EList;
import eutil.math.ENumUtil;

public class PerspectiveRenderer {
    
    //========
    // Fields
    //========
    
    private int currentWidth;
    private int currentHeight;
    private float[] zBuffer;
    private Matrix4 projection;
    private final Matrix4 view;
    private final Matrix4f tview;
    
    private Vector3 cameraUpDefault = new Vector3(0, 1, 0);
    private Vector3 cameraFrontDefault = new Vector3(0, 0, 1);
    private Vector3 cameraUp;
    private Vector3 cameraFront;
    
    private Vector3 nearPlane = new Vector3(0.0f, 0.0f, 0.01f);
    private Vector3 farPlane = new Vector3(0.0f, 0.0f, 1000.0f);
    private final Vector3 nearNorm = new Vector3(0.0f, 0.0f, 1.0f);
    private final Vector3 farNorm = new Vector3(0.0f, 0.0f, -1.0f);
    private Vector3 offsetView = new Vector3(1, 1, 0);
    
    private final Vector3 top = new Vector3(0.0f, 0.0f, 0.0f);
    private final Vector3 left = new Vector3(0.0f, 0.0f, 0.0f);
    private final Vector3 topNormal = new Vector3(0.0f, 1.0f, 0.0f);
    private final Vector3 botNormal = new Vector3(0.0f, -1.0f, 0.0f);
    private final Vector3 leftNormal = new Vector3(1.0f, 0.0f, 0.0f);
    private final Vector3 rightNormal = new Vector3(-1.0f, 0.0f, 0.0f);
    
    private float nearPlaneDist = 0.1f;
    private float farPlaneDist = 1000.0f;
    private float fov = 90.0f;
    
    private Vector3 lightDirection;
    
    //==============
    // Constructors
    //==============
    
    public PerspectiveRenderer(int screenWidth, int screenHeight) {
        currentWidth = screenWidth;
        currentHeight = screenHeight;
        
        updateProjectionMatrix();
        tview = new Matrix4f();
        view = new Matrix4();
        
        lightDirection = new Vector3(0.0f, 0.3f, -1.0f).norm();
        
        cameraUp = new Vector3(cameraUpDefault);
        cameraFront = new Vector3(cameraFrontDefault);
    }
    
    //=========
    // Methods
    //=========
    
    /**
     * Renders the given entities with the given camera position onto the
     * given graphics context.
     * 
     * @param camera   The camera to render against
     * @param entities The entities to render
     * @param canvas   The graphics context to draw to
     */
    public void render(Camera camera, EList<Entity> entities, BufferedImage canvas) {
        nearPlane = new Vector3(0.0f, 0.0f, 0.01f);
        lightDirection = new Vector3(1.0f, 0.3f, 0.0f).norm();
        
        prepareRenderer(camera);
        Box2<BoxList<Entity, EList<Vector3>>, BoxList<Entity, EList<Triangle>>> toDraw = tessellateEntities(camera, entities);
        drawTriangles(toDraw.getB(), canvas);
        drawLines(toDraw.getA(), canvas);
    }
    
    /**
     * Call to whenever the drawing area's dimensions change so that
     * projection values can be updated.
     * 
     * @param newWidth The new screen width in pixels
     * @param newHeight The new screen height in pixels
     */
    public void onScreenResized(int newWidth, int newHeight) {
        currentWidth = newWidth;
        currentHeight = newHeight;
        updateProjectionMatrix();
    }
    
    //=========
    // Getters
    //=========
    
    public float getFOV() { return fov; }
    public float getNearPlaneDist() { return nearPlaneDist; }
    public float getFarPlaneDist() { return farPlaneDist; }
    public Matrix4 getProjectionMatrix() { return projection; }
    public Matrix4 getViewMatrix() { return view; }
    
    //=========
    // Setters
    //=========
    
    public void setFOV(float fov) { this.fov = fov; updateProjectionMatrix(); }
    public void setNearPlaneDist(float near) { this.nearPlaneDist = near; updateProjectionMatrix(); }
    public void setFarPlaneDist(float far) { this.farPlaneDist = far; updateProjectionMatrix(); }
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    private void prepareRenderer(Camera camera) {
        updateViewMatrix(camera);
        updateZBuffer();
    }
    
    private void updateViewMatrix(Camera camera) {
        cameraUp.set(cameraUpDefault);
        cameraFront.set(cameraFrontDefault);
        
//        Vector3f eye = new Vector3f(camera.position.x, camera.position.y, camera.position.z);
//        Vector3f cameraFront = new Vector3f(0f, 0f, -1f);
//        Vector3f cameraUp = new Vector3f(0f, 1f, 0f);
//        
//        cameraFront.add(camera.position.x, camera.position.y, 0);
//        
//        tview.identity();
//        float rx = (float) Math.toRadians(camera.rotation.x);
//        float ry = (float) Math.toRadians(camera.rotation.y);
//        float rz = (float) Math.toRadians(camera.rotation.z);
//        tview.rotateX(rx);
//        tview.rotateY(ry);
//        tview.rotateZ(rz);
//        tview.lookAt(eye, cameraFront, cameraUp);
//        view.set(tview.m00(), tview.m01(), tview.m02(), tview.m03(),
//                 tview.m10(), tview.m11(), tview.m12(), tview.m13(),
//                 tview.m20(), tview.m21(), tview.m22(), tview.m23(),
//                 tview.m30(), tview.m31(), tview.m32(), tview.m33());
        
        tview.identity();
        float rx = (float) Math.toRadians(camera.rotation.x);
        float ry = (float) Math.toRadians(camera.rotation.y);
        float rz = (float) Math.toRadians(camera.rotation.z);
        tview.rotateXYZ(rx, ry, rz);
        cameraFront = cameraFront.add(camera.position);
        tview.lookAt(new Vector3f(camera.position.x, camera.position.y, camera.position.z),
                     new Vector3f(cameraFront.x, cameraFront.y, cameraFront.z),
                     new Vector3f(cameraUp.x, cameraUp.y, cameraUp.z));
        view.set(tview.m00(), tview.m01(), tview.m02(), tview.m03(),
                 tview.m10(), tview.m11(), tview.m12(), tview.m13(),
                 tview.m20(), tview.m21(), tview.m22(), tview.m23(),
                 tview.m30(), tview.m31(), tview.m32(), tview.m33());
        
//        view.setIdentity();
//        var rot = new Matrix4();
//        rot.rotateX((float) Math.toRadians(camera.rotation.x));
//        rot.rotateY((float) Math.toRadians(camera.rotation.y));
//        var lookDir = rot.multiply(cameraFront);
//        var target = camera.position.add(lookDir);
//        var cam = new Matrix4();
//        cam.pointAt(camera.position, target, cameraUp);
//        view.set(Matrix4.quickInverse(cam));
    }
    
    private void updateProjectionMatrix() {
        projection = Matrix4.makeProjection(fov, (float) currentHeight / (float) currentWidth, nearPlaneDist, farPlaneDist);
        zBuffer = new float[currentWidth * currentHeight];
    }
    
    /** Resets zBuffer values back to +infinity. */
    private void updateZBuffer() {
        for (int i = 0; i < zBuffer.length; i++) {
            zBuffer[i] = Float.POSITIVE_INFINITY;
        }
    }
    
    private void calculateFaceLighting(Triangle t, Vector3 normal) {
        float dp = Math.max(0.1f, lightDirection.dot(normal));
        t.color = getShade(t.color, dp);
        t.calculatedLighting = dp;
    }
    
    private Box2<BoxList<Entity, EList<Vector3>>, BoxList<Entity, EList<Triangle>>> tessellateEntities(Camera camera, EList<Entity> entities) {
        BoxList<Entity, EList<Vector3>> tessellatedPoints = new BoxList<>();
        BoxList<Entity, EList<Triangle>> tessellatedTriangles = new BoxList<>();
        for (Entity e : entities) {
            tessellatedPoints.add(e, projectPoints(camera, e));
            tessellatedTriangles.add(e, projectTriangles(camera, e));
        }
        return new Box2(tessellatedPoints, tessellatedTriangles);
    }
    
    /**
     * Performs initial model tesslation, world transformations, normal
     * culling, lighting and plane clipping.
     * 
     * @param  camera The camera viewing the given model
     * @param  entity The model to render
     * 
     * @return The entity model's tessellated points
     */
    private EList<Vector3> projectPoints(Camera camera, Entity entity) {
        EList<Vector3> r = EList.newList();
        
        Vector3 lastViewed = null;
        
        // tessellate points
        for (var point : entity.model.points) {
            // world transform
            Vector3 transformed = makeTransform(entity).multiply(point);
            Vector3 viewed = view.multiply(transformed);
            
            if (lastViewed != null) {
                Box2<Vector3, Vector3> clipped = Line3D.clipLineAgainstPlane(nearPlane, nearNorm, lastViewed, viewed);
                if (clipped != null) {
                    Vector3 a = clipped.getA();
                    Vector3 b = clipped.getB();
                    r.add(projectToScreen(a));
                    r.add(projectToScreen(b));
                }
                
//                clipped = Line3D.clipLineAgainstPlane(farPlane, farNorm, lastViewed, viewed);
//                if (clipped != null) {
//                    Vector3 a = clipped.getA();
//                    Vector3 b = clipped.getB();
//                    r.add(projectToScreen(a));
//                    r.add(projectToScreen(b));
//                }
            }
            
            lastViewed = viewed;
        }
        
        return r;
    }
    
    /**
     * Performs initial model tesslation, world transformations, normal
     * culling, lighting and plane clipping.
     * 
     * @param  camera The camera viewing the given model
     * @param  entity The model to render
     * 
     * @return The entity model's tessellated triangles
     */
    private EList<Triangle> projectTriangles(Camera camera, Entity entity) {
        EList<Triangle> r = EList.newList();
        
        // tessellate triangles
        for (var tri : entity.model.triangles) {
            // world transform
            Triangle transformed = transformTriangle(entity, tri, camera);
            Vector3 normal = transformed.calculateNormal(camera.position);
            
            // only render triangles that are actually visible from the view of the camera
            float dot = transformed.cameraRay(normal, camera.position);
            if ((entity.model.insideOut) ? dot < 0.0f : dot >= 0.0f) continue;
            
            // calcuate surface lighting
            if (!entity.model.fullBright) {
                calculateFaceLighting(transformed, normal);                
            }
            
            // transform from world space into view space and clip view triangle
            // against near plane, this could form two additional triangles
            transformed = view.multiply(transformed);
            EList<Triangle> clipped = EList.newList();
            clipped.addA(transformed.clipAgainstPlane(nearPlane, nearNorm));
            //clipped.addA(transformed.clipAgainstPlane(new Vector3(0.0f, 0.0f, 1.0f), new Vector3(0, 0, -1)));
            for (int i = 0; i < clipped.size(); i++) {
                // project from 3D to 2D and transform triangle position/dimensions to screen space
                r.add(projectToScreen(clipped.get(i)));
            }
        }
        
        return r;
    }
    
    private void drawLines(BoxList<Entity, EList<Vector3>> lines, BufferedImage canvas) {
        final Vector3 bot = new Vector3(0.0f, currentHeight - 1, 0.0f);
        final Vector3 right = new Vector3(currentWidth - 1, 0.0f, 0.0f);
        
        for (Box2<Entity, EList<Vector3>> box : lines) {
            if (!(box.getA().getModel() instanceof Line3D)) continue;
            
            Line3D lineModel = (Line3D) box.getA().getModel();
            EList<Vector3> points = box.getB();
            Vector3 last = null;
            
            for (Vector3 p : points) {
                if (last != null) {
                    for (int i = 0; i < 4; i++) {
                        Box2<Vector3, Vector3> clipped = null;
                        
                        // clip it against a plane
                        switch (i) {
                        case 0: clipped = Line3D.clipLineAgainstPlane(left , leftNormal , last, p); break;
                        case 1: clipped = Line3D.clipLineAgainstPlane(bot  , botNormal  , last, p); break;
                        case 2: clipped = Line3D.clipLineAgainstPlane(right, rightNormal, last, p); break;
                        case 3: clipped = Line3D.clipLineAgainstPlane(top  , topNormal  , last, p); break;
                        }
                        
                        if (clipped != null) {
                            Vector3 a = clipped.getA();
                            Vector3 b = clipped.getB();
                            if (lineModel.antiAlias) rasterizeAALine(a, b, lineModel.lineWidth, lineModel.lineColor, canvas);
                            else rasterizeLine(a, b, lineModel.lineWidth, lineModel.lineColor, canvas);
//                            var g2d = canvas.createGraphics();
//                            g2d.setColor(Color.MAGENTA);
//                            g2d.fillOval((int) a.x - 8, (int) a.y - 8, 16, 16);
//                            g2d.fillOval((int) b.x - 8, (int) b.y - 8, 16, 16);
                        }
                    }
                }
                last = p;
            }
        }
    }
    
    private void drawTriangles(BoxList<Entity, EList<Triangle>> entityTriangles, BufferedImage canvas) {
        for (Box2<Entity, EList<Triangle>> box : entityTriangles) {
            @SuppressWarnings("unused")
            Entity e = box.getA();
            EList<Triangle> triangles = box.getB();
            
            for (Triangle t : triangles) {
                EList<Triangle> clipped = t.clipAgainstScreen(currentWidth, currentHeight);
                for (Triangle c : clipped) {
                    if (c.texture != null) rasterizeTexturedTriangle(c, canvas);
                    else rasterizeTriangle(c, canvas);
                }
            }
        }
    }
    
    private void rasterizeLine(Vector3 start, Vector3 end, int lineWidth, Color color, BufferedImage canvas) {
        final int w = currentWidth;
        final int h = currentHeight;
        int baseColor = color.getRGB();
        
        for (int i = 0; i < (int) lineWidth; i++) {
            int mx = 0, my = i;
            //if (Math.abs(end.x - start.x) < Math.abs(end.y - start.y)) {
            //    mx = i;
            //    my = 0;
            //}
            
            int x0 = (int) start.x + mx;
            int y0 = (int) start.y + my;
            int x1 = (int) end.x + mx;
            int y1 = (int) end.y + my;
            
            int dx =  Math.abs(x1 - x0), sx = (x0 < x1) ? 1 : -1;
            int dy = -Math.abs(y1 - y0), sy = (y0 < y1) ? 1 : -1;
            int err = dx + dy, e2;
            
//            System.out.println(x0 + " : " + x1 + " : " + y0 + " : " + y1);
            //System.out.println(dx + " : " + dy);
            
            while (true) {
                if (x0 == x1 && y0 == y1) break;
                if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
                    float tx = ENumUtil.clamp((x0 - start.x) / (end.x - start.x), 0.0f, 1.0f);
                    float ty = ENumUtil.clamp((y0 - start.y) / (end.y - start.y), 0.0f, 1.0f);
                    float t = (tx + ty) / 2;
                    //float t = tx;
                    float depth = (end.z - start.z) * t + start.z;
                    System.out.println("DRAW: " + x0 + " : " + y0 + " | " + tx + " : " + ty + " : " + t + " : " + depth);
                    drawPixel(x0, y0, baseColor, depth, canvas);
                }
                e2 = 2 * err;
                if (e2 > dy) { err += dy; x0 += sx; }
                if (e2 < dx) { err += dx; y0 += sy; }
            }
        }
    }
    
    /** Rasterize an anti-aliased line of variable width. */
    private void rasterizeAALine(Vector3 start, Vector3 end, float lineWidth, Color color, BufferedImage canvas) {
        final int w = currentWidth;
        final int h = currentHeight;
        final int baseColor = color.getRGB();
        
        int x0 = (int) start.x;
        int y0 = (int) start.y;
        int x1 = (int) end.x;
        int y1 = (int) end.y;
        
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx + dy;
        int e2;
        
        while (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
            float tx = (x0 - start.x) / (end.x - start.x);
            float ty = (y0 - start.y) / (end.y - start.y);
            float t = (tx + ty) / 2;
            float depth = (end.z - start.z) * t + start.z;
            
            drawPixel(x0, y0, baseColor, depth, canvas);
            if (x0 == x1 && y0 == y1) break;
            
            e2 = 2 * err;
            
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
        
    }
    
    public static int blend(int src_color, int dst_color) {
        float src_a = ((src_color >> 24) & 0xFF) / 255f;
        float src_r = ((src_color >> 16) & 0xFF) / 255f;
        float src_g = ((src_color >> 8) & 0xFF) / 255f;
        float src_b = ((src_color) & 0xFF) / 255f;
        
        float dst_a = ((dst_color >> 24) & 0xFF) / 255f;
        float dst_r = ((dst_color >> 16) & 0xFF) / 255f;
        float dst_g = ((dst_color >> 8) & 0xFF) / 255f;
        float dst_b = ((dst_color) & 0xFF) / 255f;
        
        float out_a = (src_a * src_a) + (dst_a * (1 - src_a));
        float out_r = (src_r * src_a) + (dst_r * (1 - src_a));
        float out_g = (src_g * src_a) + (dst_g * (1 - src_a));
        float out_b = (src_b * src_a) + (dst_b * (1 - src_a));
        
        out_a = (int) (out_a * 255f);
        out_r = (int) (out_r * 255f);
        out_g = (int) (out_g * 255f);
        out_b = (int) (out_b * 255f);
        
        return ((int) out_a << 24) | ((int) out_r << 16) | ((int) out_g << 8) | (int) out_b;
    }
    
    private void rasterizeTriangle(Triangle t, BufferedImage canvas) {
        Vector3 v0 = t.v0.pos, v1 = t.v1.pos, v2 = t.v2.pos;
        
        int minX = (int) Math.max(0, Math.ceil(Math.min(v0.x, Math.min(v1.x, v2.x))));
        int maxX = (int) Math.min(currentWidth - 1, Math.floor(Math.max(v0.x, Math.max(v1.x, v2.x))));
        int minY = (int) Math.max(0, Math.ceil(Math.min(v0.y, Math.min(v1.y, v2.y))));
        int maxY = (int) Math.min(currentHeight - 1, Math.floor(Math.max(v0.y, Math.max(v1.y, v2.y))));
        
        float triangleArea = (v0.y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - v0.x);
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float b1 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                float b2 = ((y - v0.y) * (v2.x - v0.x) + (v2.y - v0.y) * (v0.x - x)) / triangleArea;
                float b3 = ((y - v1.y) * (v0.x - v1.x) + (v0.y - v1.y) * (v1.x - x)) / triangleArea;
                // only draw pixel if it is within the bounds of the triangle's vertices
                if (b1 < 0 || b1 > 1 || b2 < 0 || b2 > 1 || b3 < 0 || b3 > 1) continue;
                float depth = b1 * v0.z + b2 * v1.z + b3 * v2.z;
                int rgb = t.color.getRGB();
                drawPixel(x, y, rgb, depth, canvas);
            }
        }
    }
    
    private void rasterizeTexturedTriangle(Triangle tri, BufferedImage canvas) {
        final BufferedImage tex = tri.texture;
        final int texWidth = tex.getWidth() - 1;
        final int texHeight = tex.getHeight() - 1;
        
        int x0 = (int) tri.v0.pos.x, y0 = (int) tri.v0.pos.y;
        int x1 = (int) tri.v1.pos.x, y1 = (int) tri.v1.pos.y;
        int x2 = (int) tri.v2.pos.x, y2 = (int) tri.v2.pos.y;
        float u0 = tri.v0.tex.x, v0 = tri.v0.tex.y, w0 = tri.v0.tex.w;
        float u1 = tri.v1.tex.x, v1 = tri.v1.tex.y, w1 = tri.v1.tex.w;
        float u2 = tri.v2.tex.x, v2 = tri.v2.tex.y, w2 = tri.v2.tex.w;
        
        float area = calcuateArea(tri);
        
        if (y1 < y0) {
            int ty = y0; y0 = y1; y1 = ty;
            int tx = x0; x0 = x1; x1 = tx;
            float tu = u0; u0 = u1; u1 = tu;
            float tv = v0; v0 = v1; v1 = tv;
            float tw = w0; w0 = w1; w1 = tw;
        }
        if (y2 < y0) {
            int ty = y0; y0 = y2; y2 = ty;
            int tx = x0; x0 = x2; x2 = tx;
            float tu = u0; u0 = u2; u2 = tu;
            float tv = v0; v0 = v2; v2 = tv;
            float tw = w0; w0 = w2; w2 = tw;
        }
        if (y2 < y1) {
            int ty = y1; y1 = y2; y2 = ty;
            int tx = x1; x1 = x2; x2 = tx;
            float tu = u1; u1 = u2; u2 = tu;
            float tv = v1; v1 = v2; v2 = tv;
            float tw = w1; w1 = w2; w2 = tw;
        }
        
        int dx1 = x1 - x0;
        int dy1 = y1 - y0;
        float du1 = u1 - u0;
        float dv1 = v1 - v0;
        float dw1 = w1 - w0;
        
        int dx2 = (int) (x2 - x0);
        int dy2 = (int) (y2 - y0);
        float du2 = u2 - u0;
        float dv2 = v2 - v0;
        float dw2 = w2 - w0;
        
        float daxStep = 0, dbxStep = 0,
              du1Step = 0, dv1Step = 0,
              du2Step = 0, dv2Step = 0,
              dw1Step = 0, dw2Step = 0;
        
        if (dy1 != 0) daxStep = dx1 / (float) Math.abs(dy1);
        if (dy2 != 0) dbxStep = dx2 / (float) Math.abs(dy2);
        
        if (dy1 != 0) du1Step = du1 / (float) Math.abs(dy1);
        if (dy1 != 0) dv1Step = dv1 / (float) Math.abs(dy1);
        if (dy1 != 0) dw1Step = dw1 / (float) Math.abs(dy1);
        
        if (dy2 != 0) du2Step = du2 / (float) Math.abs(dy2);
        if (dy2 != 0) dv2Step = dv2 / (float) Math.abs(dy2);
        if (dy2 != 0) dw2Step = dw2 / (float) Math.abs(dy2);
        
        if (dy1 != 0) {
            for (int i = y0; i <= y1; i++) {
                int ax = (int) (x0 + (float) (i - y0) * daxStep);
                int bx = (int) (x0 + (float) (i - y0) * dbxStep);
                
                float tsu = u0 + (float) (i - y0) * du1Step;
                float tsv = v0 + (float) (i - y0) * dv1Step;
                float tsw = w0 + (float) (i - y0) * dw1Step;
                
                float teu = u0 + (float) (i - y0) * du2Step;
                float tev = v0 + (float) (i - y0) * dv2Step;
                float tew = w0 + (float) (i - y0) * dw2Step;
                
                if (ax > bx) {
                    int tx = ax; ax = bx; bx = tx;
                    float tu = tsu; tsu = teu; teu = tu;
                    float tv = tsv; tsv = tev; tev = tv;
                    float tw = tsw; tsw = tew; tew = tw;
                }
                
                float texU = tsu;
                float texV = tsv;
                float texW = tsw;
                
                float tstep = 1.0f / ((float) (bx - ax));
                float t = 0.0f;
                
                for (int j = ax; j < bx; j++) {
                    texU = (1.0f - t) * tsu + t * teu;
                    texV = (1.0f - t) * tsv + t * tev;
                    texW = (1.0f - t) * tsw + t * tew;
                    
                    int x = (int) ((texU / texW) * (float) texWidth);
                    int y = (int) ((texV / texW) * (float) texHeight);
                    x = ENumUtil.clamp(x, 0, texWidth - 1);
                    y = ENumUtil.clamp(y, 0, texHeight - 1);
                    int rgb = tri.texture.getRGB(x, y);
                    int color = EColors.changeBrightness(rgb, (int) (tri.calculatedLighting * 255f));
                    float depth = calculateDepth(j, i, tri, area);
                    drawPixel(j, i, color, depth, canvas);
                    
                    t += tstep;
                }
            }
        }
        
        dx1 = x2 - x1;
        dy1 = y2 - y1;
        du1 = u2 - u1;
        dv1 = v2 - v1;
        dw1 = w2 - w1;
        
        if (dy1 != 0) daxStep = dx1 / (float) Math.abs(dy1);
        if (dy2 != 0) dbxStep = dx2 / (float) Math.abs(dy2);
        
        du1Step = 0; dv1Step = 0;
        if (dy1 != 0) du1Step = du1 / (float) Math.abs(dy1);
        if (dy1 != 0) dv1Step = dv1 / (float) Math.abs(dy1);
        if (dy1 != 0) dw1Step = dw1 / (float) Math.abs(dy1);
        
        if (dy1 != 0) {
            for (int i = y1; i <= y2; i++) {
                int ax = (int) (x1 + (float) (i - y1) * daxStep);
                int bx = (int) (x0 + (float) (i - y0) * dbxStep);
                
                float tsu = u1 + (float) (i - y1) * du1Step;
                float tsv = v1 + (float) (i - y1) * dv1Step;
                float tsw = w1 + (float) (i - y1) * dw1Step;
                
                float teu = u0 + (float) (i - y0) * du2Step;
                float tev = v0 + (float) (i - y0) * dv2Step;
                float tew = w0 + (float) (i - y0) * dw2Step;
                
                if (ax > bx) {
                    int tx = ax; ax = bx; bx = tx;
                    float tu = tsu; tsu = teu; teu = tu;
                    float tv = tsv; tsv = tev; tev = tv;
                    float tw = tsw; tsw = tew; tew = tw;
                }
                
                float texU = tsu;
                float texV = tsv;
                float texW = tsw;
                
                float tstep = 1.0f / ((float) (bx - ax));
                float t = 0.0f;
                
                for (int j = ax; j <= bx; j++) {
                    texU = (1.0f - t) * tsu + t * teu;
                    texV = (1.0f - t) * tsv + t * tev;
                    texW = (1.0f - t) * tsw + t * tew;
                    
                    int x = (int) ((texU / texW) * (float) texWidth);
                    int y = (int) ((texV / texW) * (float) texHeight);
                    x = ENumUtil.clamp(x, 0, texWidth - 1);
                    y = ENumUtil.clamp(y, 0, texHeight - 1);
                    int rgb = tri.texture.getRGB(x, y);
                    int color = EColors.changeBrightness(rgb, (int) (tri.calculatedLighting * 255f));
                    float depth = calculateDepth(j, i, tri, area);
                    drawPixel(j, i, color, depth, canvas);
                    
                    t += tstep;
                }
            }
        }
    }
    
    private float calcuateArea(Triangle t) {
        Vector3 v0 = t.v0.pos, v1 = t.v1.pos, v2 = t.v2.pos;
        return (v0.y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - v0.x);
    }
    private float calculateDepth(int x, int y, Triangle t, float area) {
        Vector3 v0 = t.v0.pos, v1 = t.v1.pos, v2 = t.v2.pos;
        float b1 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / area;
        float b2 = ((y - v0.y) * (v2.x - v0.x) + (v2.y - v0.y) * (v0.x - x)) / area;
        float b3 = ((y - v1.y) * (v0.x - v1.x) + (v0.y - v1.y) * (v1.x - x)) / area;
        return b1 * v0.z + b2 * v1.z + b3 * v2.z;
    }
    
    /** Draws a single pixel at the given x/y at the given depth onto the given image. */
    private void drawPixel(int x, int y, int color, float depth, BufferedImage canvas) {
        int zIndex = y * currentWidth + x;
        if (zBuffer[zIndex] >= depth) {
            int dst = canvas.getRGB(x, y);
            color = blend(color, dst);
            canvas.setRGB(x, y, color);
            zBuffer[zIndex] = depth;
        }
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    private Triangle transformTriangle(Entity e, Triangle t, Camera camera) {
        Triangle out = makeTransform(e).multiply(t);
        if (t.alwaysFaceCamera) {
            Vector3 v0 = out.v0();
            Vector3 v1 = out.v1();
            Vector3 v2 = out.v2();
            
            Vector3 cameraRight = new Vector3(view.m00, view.m10, view.m20);
            Vector3 cameraUp = new Vector3(view.m01, view.m11, view.m21);
            
            
        }
        return out;
    }
    
    private static Matrix4 makeTransform(Entity e) {
        return makeTransform(e.position, e.rotation, e.scale);
    }
    
    private static Matrix4 makeTransform(Vector3 pos, Vector3 rot, Vector3 scale) {
        Matrix4 m = new Matrix4();
        m.scale(scale);
        m.rotateXYZ(rot);
        m.translate(pos);
        return m;
    }
    
    private Triangle projectToScreen(Triangle tri) {
        Triangle t = projection.multiply(tri);
        
        t.v0.tex.x = t.v0.tex.x / t.v0.pos.w;
        t.v1.tex.x = t.v1.tex.x / t.v1.pos.w;
        t.v2.tex.x = t.v2.tex.x / t.v2.pos.w;
        
        t.v0.tex.y = t.v0.tex.y / t.v0.pos.w;
        t.v1.tex.y = t.v1.tex.y / t.v1.pos.w;
        t.v2.tex.y = t.v2.tex.y / t.v2.pos.w;
        
        t.v0.tex.w = 1.0f / t.v0.pos.w;
        t.v1.tex.w = 1.0f / t.v1.pos.w;
        t.v2.tex.w = 1.0f / t.v2.pos.w;
        
        // scale into view
        t.v0.pos = t.v0.pos.div(t.v0.pos.w);
        t.v1.pos = t.v1.pos.div(t.v1.pos.w);
        t.v2.pos = t.v2.pos.div(t.v2.pos.w);
        // x/y are inverted so put them back
        t.v0.pos.x *= -1.0f; t.v0.pos.y *= -1.0f;
        t.v1.pos.x *= -1.0f; t.v1.pos.y *= -1.0f;
        t.v2.pos.x *= -1.0f; t.v2.pos.y *= -1.0f;
        // offset verts into visible normalized space
        t.v0.pos = t.v0.pos.add(offsetView);
        t.v1.pos = t.v1.pos.add(offsetView);
        t.v2.pos = t.v2.pos.add(offsetView);
        t.v0.pos.x *= 0.5f * currentWidth; t.v0.pos.y *= 0.5f * currentHeight;
        t.v1.pos.x *= 0.5f * currentWidth; t.v1.pos.y *= 0.5f * currentHeight;
        t.v2.pos.x *= 0.5f * currentWidth; t.v2.pos.y *= 0.5f * currentHeight;
        
        return t;
    }
    
    private Vector3 projectToScreen(Vector3 point) {
        Vector3 p = projection.multiply(point);
        // scale into view
        p = p.div(p.w);
        // x/y are inverted so put them back
        p.x *= -1.0f; p.y *= -1.0f;
        // offset vert into visible normalized space
        p = p.add(offsetView);
        p.x *= 0.5f * currentWidth;
        p.y *= 0.5f * currentHeight;
        return p;
    }
    
    private static Color getShade(Color color, float shade) {
        double redLinear = Math.pow(color.getRed(), 2.0) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.0) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.0) * shade;

        int red = (int) Math.pow(redLinear, 1 / 2.0);
        int green = (int) Math.pow(greenLinear, 1 / 2.0);
        int blue = (int) Math.pow(blueLinear, 1 / 2.0);

        return new Color(red, green, blue);
    }
    
//    {
//        final int dx = Math.abs(x1 - x0), sx = (x0 < x1) ? 1 : -1;
//        final int dy = Math.abs(y1 - y0), sy = (y0 < y1) ? 1 : -1;
//        final float ed = (dx + dy == 0) ? 1.0f : (float) Math.sqrt(dx * dx + dy * dy);
//        int err = dx + dy, e2, x2, y2;
//        int alpha, out_color;
//        
//        //System.out.println("\nLINE");
//        final float wd = (lineWidth + 1) / 2.0f;
//        while (true) {
//            float tx = (x0 - start.x) / (end.x - start.x);
//            float ty = (y0 - start.y) / (end.y - start.y);
//            float t = (tx + ty) / 2;
//            float depth = (end.z - start.z) * t + start.z;
//            
//            if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
//                alpha = (int) Math.max(0, 255.0f * Math.abs(err - dx + dy) / ed - wd + 1);
//                //System.out.println(err + " : " + dx + " : " + dy + " => " + alpha);
//                int src_color = EColors.changeOpacity(baseColor, alpha);
//                int dst_color = canvas.getRGB(x0, y0);
//                out_color = blend(src_color, dst_color);
//                drawPixel(x0, y0, out_color, depth, canvas);
//            }
//            
//            e2 = err;
//            x2 = x0;
//            
//            if (2 * e2 >= -dx) {
//                for (e2 += dy, y2 = y0; (e2 < (ed * wd)) && (y1 != y2 || dx > dy); e2 += dx) {
//                    y2 += sy;
//                    if (x0 >= 0 && x0 < w && y2 >= 0 && y2 < h) {
//                        alpha = (int) Math.max(0, 255.0f * Math.abs(e2) / ed - wd + 1);
//                        int src_color = EColors.changeOpacity(baseColor, alpha);
//                        int dst_color = canvas.getRGB(x0, y2);
//                        out_color = blend(src_color, dst_color);
//                        drawPixel(x0, y2, out_color, depth, canvas);
//                    }
//                }
//                if (x0 == x1) break;
//                //System.out.println("ERR: " + err);
//                e2 = err;
//                err -= dy;
//                x0 += sx;
//            }
//            if (2 * e2 <= dy) {
//                for (e2 = dx - e2; (e2 < (ed * wd)) && (x1 != x2 || dx < dy); e2 += dy) {
//                    x2 += sx;
//                    if (x2 >= 0 && x2 < w && y0 >= 0 && y0 < h) {
//                        alpha = (int) Math.max(0, 255 - 255.0f * Math.abs(e2) / ed - wd + 1);
//                        int src_color = EColors.changeOpacity(baseColor, alpha);
//                        int dst_color = canvas.getRGB(x0, y0);
//                        out_color = blend(src_color, dst_color);
//                        //drawPixel(x2, y0, out_color, depth, canvas);
//                    }
//                }
//                if (y0 == y1) break;
//                err += dx;
//                y0 += sy;
//            }
//        }
//    }
    
}
