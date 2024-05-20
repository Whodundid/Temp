package controller.globe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import eutil.colors.EColors;
import eutil.datatypes.boxes.Box2;
import eutil.datatypes.util.EList;

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
    private Vector3 farPlane = new Vector3(0.0f, 0.0f, 1.0f);
    private Vector3 offsetView = new Vector3(1, 1, 0);
    
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
        nearPlane = new Vector3(0.0f, 0.0f, 0.002f);
        prepareRenderer(camera);
        Box2<EList<Vector3>, EList<Triangle>> toDraw = tessellateEntities(camera, entities);
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
        tview.identity();
        tview.rotateX((float) Math.toRadians(camera.rotation.x));
        tview.rotateY((float) Math.toRadians(camera.rotation.y));
        tview.rotateZ((float) Math.toRadians(camera.rotation.z));
        cameraFront = cameraFront.add(camera.position);
        tview.lookAt(new Vector3f(camera.position.x, camera.position.y, camera.position.z),
                  new Vector3f(cameraFront.x, cameraFront.y, cameraFront.z),
                  new Vector3f(cameraUp.x, cameraUp.y, cameraUp.z));
        view.set(tview.m00(), tview.m01(), tview.m02(), tview.m03(),
                 tview.m10(), tview.m11(), tview.m12(), tview.m13(),
                 tview.m20(), tview.m21(), tview.m22(), tview.m23(),
                 tview.m30(), tview.m31(), tview.m32(), tview.m33());
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
    
    private Box2<EList<Vector3>, EList<Triangle>> tessellateEntities(Camera camera, EList<Entity> entities) {
        EList<Vector3> tessellatedPoints = EList.newList();
        EList<Triangle> tessellatedTriangles = EList.newList();
        for (Entity e : entities) {
            tessellatedPoints.addAll(tessellatePoints(camera, e));
            tessellatedTriangles.addAll(tessellateTriangles(camera, e));
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
    private EList<Vector3> tessellatePoints(Camera camera, Entity entity) {
        EList<Vector3> r = EList.newList();
        
        // tessellate points
        for (var point : entity.model.points) {
            // world transform
            Vector3 transformed = makeTransform(entity).multiply(point);
            Vector3 viewed = view.multiply(transformed);
            Vector3 projected = projection.multiply(viewed);
            // scale into view
            projected = projected.div(projected.w);
            // x/y are inverted so put them back
            projected.x *= -1.0f; projected.y *= -1.0f;
            // offset vert into visible normalized space
            projected = projected.add(offsetView);
            projected.x *= 0.5f * currentWidth;
            projected.y *= 0.5f * currentHeight;
            
            r.add(projected);
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
    private EList<Triangle> tessellateTriangles(Camera camera, Entity entity) {
        EList<Triangle> r = EList.newList();
        
        // tessellate triangles
        for (var tri : entity.model.triangles) {
            // world transform
            Triangle transformed = transformTriangle(entity, tri);
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
            Triangle[] clipped = view.multiply(transformed).clipAgainstPlane(nearPlane, farPlane);
            for (int i = 0; i < clipped.length; i++) {
                // project from 3D to 2D and transform triangle position/dimensions to screen space
                r.add(projectToScreen(clipped[i], projection, offsetView, currentWidth, currentHeight));
            }
        }
        
        return r;
    }
    
    private void drawLines(EList<Vector3> points, BufferedImage canvas) {
        Vector3 last = null;
        //System.out.println(points);
        for (Vector3 p : points) {
            //EList<Triangle> clipped = p.clipAgainstScreen(currentWidth, currentHeight);
            //for (Triangle c : clipped) {
            if (last != null) {
                rasterizeLine(last.x, last.y, last.z, p.x, p.y, p.z, 2.0f, canvas);
            }
            //}
            last = p;
        }
    }
    
    private void drawTriangles(EList<Triangle> triangles, BufferedImage canvas) {
        for (Triangle t : triangles) {
            EList<Triangle> clipped = t.clipAgainstScreen(currentWidth, currentHeight);
            for (Triangle c : clipped) {
                if (c.texture != null) rasterizeTexturedTriangle(c, canvas);
                else rasterizeTriangle(c, canvas);
            }
        }
    }
    
    private void rasterizeLine(float startX, float startY, float startZ,
                               float endX, float endY, float endZ,
                               float lineWidth, BufferedImage canvas)
    {
        Graphics2D g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawLine((int) startX, (int) startY, (int) endX, (int) endY);
    }
    
    private void rasterizeTriangle(Triangle t, BufferedImage canvas) {
        Vector3 v0 = t.v0.pos, v1 = t.v1.pos, v2 = t.v2.pos;
        
        int minX = (int) Math.max(0, Math.ceil(Math.min(v0.x, Math.min(v1.x, v2.x))));
        int maxX = (int) Math.min(currentWidth - 1, Math.floor(Math.max(v0.x, Math.max(v1.x, v2.x))));
        int minY = (int) Math.max(0, Math.ceil(Math.min(v0.y, Math.min(v1.y, v2.y))));
        int maxY = (int) Math.min(currentHeight - 1, Math.floor(Math.max(v0.y, Math.max(v1.y, v2.y))));
        
        float triangleArea = calcuateArea(t);
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float b1 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                float b2 = ((y - v0.y) * (v2.x - v0.x) + (v2.y - v0.y) * (v0.x - x)) / triangleArea;
                float b3 = ((y - v1.y) * (v0.x - v1.x) + (v0.y - v1.y) * (v1.x - x)) / triangleArea;
                // only draw pixel if it is within the bounds of the triangle's vertices
                if (b1 < 0 || b1 > 1 || b2 < 0 || b2 > 1 || b3 < 0 || b3 > 1) continue;
                float depth = b1 * v0.z + b2 * v1.z + b3 * v2.z;
                drawPixel(x, y, t.color.getRGB(), depth, canvas);
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
                
                for (int j = ax; j < bx; j++) {
                    texU = (1.0f - t) * tsu + t * teu;
                    texV = (1.0f - t) * tsv + t * tev;
                    texW = (1.0f - t) * tsw + t * tew;
                    
                    int x = (int) ((texU / texW) * (float) texWidth);
                    int y = (int) ((texV / texW) * (float) texHeight);
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
            
            canvas.setRGB(x, y, color);
            zBuffer[zIndex] = depth;
        }
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    private static Triangle transformTriangle(Entity e, Triangle t) {
        return makeTransform(e).multiply(t);
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
    
    private static Triangle projectToScreen(Triangle tri, Matrix4 projection, Vector3 offsetView,
                                           int screenWidth, int screenHeight)
    {
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
        
        t.positionInWindow(offsetView, screenWidth, screenHeight);
        return t;
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
    
}
