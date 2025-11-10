package controller.globe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import controller.globe.math.Matrix4;
import controller.globe.math.Vector3;
import controller.globe.models.Line3D;
import controller.globe.models.Model;
import controller.globe.models.Triangle;
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
    private int canvasWidth;
    private int canvasHeight;
    private float[] zBuffer;
    private Matrix4 projection;
    private Matrix4 view;
    private float lightDegrees = 0.0f;
    
    private Vector3 cameraUpDefault = new Vector3(0, 1, 0);
    private Vector3 cameraFrontDefault = new Vector3(0, 0, 1);
    private Vector3 cameraUp = new Vector3(cameraUpDefault);
    private Vector3 cameraFront = new Vector3(cameraFrontDefault);
    
    private Vector3 nearPlane = new Vector3(0.0f, 0.0f, 0.003f);
    private final Vector3 nearNorm = new Vector3(0.0f, 0.0f, 1.0f);
    private Vector3 offsetView = new Vector3(1.0f, 1.0f, 0.0f);
    
    private final Vector3 top = new Vector3(0.0f, 0.0f, 0.0f);
    private final Vector3 left = new Vector3(0.0f, 0.0f, 0.0f);
    private final Vector3 bot = new Vector3();
    private final Vector3 right = new Vector3();
    private final Vector3 topNormal = new Vector3(0.0f, 1.0f, 0.0f);
    private final Vector3 botNormal = new Vector3(0.0f, -1.0f, 0.0f);
    private final Vector3 leftNormal = new Vector3(1.0f, 0.0f, 0.0f);
    private final Vector3 rightNormal = new Vector3(-1.0f, 0.0f, 0.0f);
    
    private float nearPlaneDist = 0.01f;
    private float farPlaneDist = 1000.0f;
    private float fov = 70.0f;
    
    private Vector3 lightDirection;
    private boolean enableBlend;
    /** Super-Sample Anti-Aliasing --> Render at 2x canvas size and downscale to canvas. */
    private boolean useSSAA = false;
    private int[] pixelBuffer;    
    //==============
    // Constructors
    //==============
    
    public PerspectiveRenderer(int screenWidth, int screenHeight) {
        currentWidth = screenWidth;
        currentHeight = screenHeight;
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
    public void render(Camera camera, EList<Entity> entities, BufferedImage canvas, int screenWidth, int screenHeight, boolean enableBlend, boolean useSSAA) {
        if (screenWidth == 0 || screenHeight == 0 || canvas == null || canvas.getWidth() == 0 || canvas.getHeight() == 0) return;
        
        this.enableBlend = enableBlend;
        this.useSSAA = useSSAA;
        
        entities = entities.copy();
        
        // sort entities so that transparent entities are last in the list
        EList<Entity> transparentEntities = entities.filter(e -> {
            return e.model != null && e.model.isTransparent();
        });
        
        // remove all transparent entities
        entities.removeAll(transparentEntities);
        // add them all back but now to the end of the list
        entities.addAll(transparentEntities);
        
        BufferedImage drawingCanvas = canvas;
//        boolean useMultithreadig = false;
        
        // Check if SSAA is enabled
        // SSAA => Super Sampled Anti-Aliasing
        if (this.useSSAA) {
            // Create a scaled image for SSAA
//            final double ssaaScaler = 1.05;
            final double ssaaScaler = 2;
            int dw = (int) (currentWidth * ssaaScaler);
            int dh = (int) (currentHeight * ssaaScaler);
            BufferedImage ssaaCanvas = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
            drawingCanvas = ssaaCanvas;
        }

        pixelBuffer = ((DataBufferInt) drawingCanvas.getRaster().getDataBuffer()).getData();
        
        canvasWidth = drawingCanvas.getWidth();
        canvasHeight = drawingCanvas.getHeight();
        
        bot.set(0.0f, canvasHeight - 1.0f, 0.0f);
        right.set(canvasWidth - 1.0f, 0.0f, 0.0f);
        
        // Render the scene to the larger canvas
        updateProjectionMatrix();
        prepareRenderer(camera);
        
//        if (useMultithreadig) {
//            renderMultithreaded(entities);
//        }
//        else {
            for (Entity e : entities) {
                renderEntity(camera, e);
            }
//        }
        
        // Downsample the image to fit the original canvas size
        if (this.useSSAA) {
            Graphics2D g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(drawingCanvas, 0, 0, currentWidth, currentHeight, 0, 0, canvasWidth, canvasHeight, null);
            g2d.dispose();
        }
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
    }
    
    //=========
    // Getters
    //=========
    
    public float getFOV() { return fov; }
    public float getNearPlaneDist() { return nearPlaneDist; }
    public float getFarPlaneDist() { return farPlaneDist; }
    public Matrix4 getProjectionMatrix() { return projection; }
    public Matrix4 getViewMatrix() { return view; }
    public Vector3 getLightDirection() { return this.lightDirection; }
    public float getLightDegrees() { return this.lightDegrees; }
    
    //=========
    // Setters
    //=========
    
    public void setFOV(float fov) { this.fov = fov; updateProjectionMatrix(); }
    public void setNearPlaneDist(float near) { this.nearPlaneDist = near; updateProjectionMatrix(); }
    public void setFarPlaneDist(float far) { this.farPlaneDist = far; updateProjectionMatrix(); }
    public void setLightDirection(Vector3 dir) { this.lightDirection = dir; }
    public void setLightDegrees(float degrees) { this.lightDegrees = degrees; }
    
    //==================
    // Internal Methods
    //==================
    
    private void prepareRenderer(Camera camera) {
        updateViewMatrix(camera);
        updateZBuffer();
        
        lightDirection = new Vector3(1.0f, 0.0f, 0.0f).normalize();
        Matrix4 rot = Matrix4.makeRotationY((float) Math.toRadians(lightDegrees));
        lightDirection = rot.multiply(lightDirection);
    }
    
//    private void renderMultithreaded(EList<Entity> entities) {
//        WorkerThread[] threads = null;
//          
//        threads = new WorkerThread[4];
//        BoxList<Entity, EList<Vector3>> lines = toDraw.getA();
//        BoxList<Entity, EList<Triangle>> triangles = toDraw.getB();
//        
//        int totalLinePoints = 0;
//        int totalTriangles = 0;
//        
//        for (var p : lines.getBVals()) {
//            totalLinePoints += p.size();
//        }
//        for (var t : triangles.getBVals()) {
//            totalTriangles += t.size();
//        }
//        
//        int linePointsPerThread = totalLinePoints / threads.length;
//        int trianglesPerThread = totalTriangles / threads.length;
//        
//        for (int i = 0; i < threads.length; i++) {
//            BoxList<Entity, EList<Vector3>> workerLines = new BoxList<>();
//            BoxList<Entity, EList<Triangle>> workerTriangles = new BoxList<>();
//            
//            if (linePointsPerThread > 0) {
//                for (int q = 0, curLineIndex = 0, linePointIndex = 0; q < linePointsPerThread; q++) {
//                    Box2<Entity, EList<Vector3>> curLine = lines.get(curLineIndex);
//                    
//                }
//            }
//            if (trianglesPerThread > 0) {
//                
//            }
//        }
//    }
    
//    private void updateViewMatrix(Camera camera) {
//        cameraUp.set(cameraUpDefault);
//        cameraFront.set(cameraFrontDefault);
//        Matrix4 cameraRot = new Matrix4().rotateXYZ_Degrees(camera.rotation);
//        Vector3 lookDir = cameraRot.multiply(cameraFront);
//        cameraFront = camera.position.add(lookDir);
//        Matrix4 cameraMat = Matrix4.pointAt(camera.position, cameraFront, cameraUp);
//        view = cameraMat.inverse();
//    }
    
    private static Vector3 rotateAroundAxis(Vector3 v, Vector3 axis, float angleRad) {
        // Rodrigues' rotation formula
        Vector3 k = new Vector3(axis).normalize();
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        return v.mul(c)
                .add(k.cross(v).mul(s))
                .add(k.mul(k.dot(v) * (1 - c)));
    }

    private void updateViewMatrix(Camera camera) {
        view = camera.getViewMatrix();
    }
    
    private void updateProjectionMatrix() {
        projection = Matrix4.projection(fov, (float) canvasWidth / (float) canvasHeight, nearPlaneDist, farPlaneDist);
        zBuffer = new float[canvasWidth * canvasHeight];
    }
    
    /** Resets zBuffer values back to +infinity. */
    private void updateZBuffer() {
        for (int i = 0; i < zBuffer.length; i++) {
            zBuffer[i] = Float.POSITIVE_INFINITY;
        }
    }
    
    private void calculateSurfaceLighting(Model m, Triangle t, Vector3 normal) {
        float dp = Math.max(0.1f, lightDirection.dot(normal));
        dp = ENumUtil.clamp(dp, m.minimumBrightness, 1.0f);
        if (t.color != null) {
            t.color = getShade(t.color, dp);
        }
        t.calculatedLighting = dp;
    }
    
    private void renderEntity(Camera camera, Entity entity) {
        if (entity.hidden) return;
        if (entity.model == null) return;
        if (!entity.model.visible) return;
        
        var transform = makeTransform(entity);
        renderModel(camera, entity.model, transform);
    }
    
    /**
     * Tessellates all points and triangles for a specific model and
     * recursively for all child models.
     * 
     * @param camera The camera viewing the entity's model
     * @param m The model
     * @param transform The entity in scaled world space
     */
    private void renderModel(Camera camera, Model m, Matrix4 transform) {
        // tessellate model's children
        for (Model child : m.childModels) {
            renderModel(camera, child, transform);
        }
        
        // tessellate the parent model itself
        if (m instanceof Line3D line) {
            renderLine(camera, line, transform);
        }
        else {
            renderTriangles(camera, m, transform);
        }
    }
    
    /**
     * Performs initial model tesslation, world transformations, normal
     * culling, lighting and plane clipping.
     * 
     * @param camera The camera viewing the given model
     * @param m The line model to render
     * @param transform the world transform of the parent entity
     */
    private void renderLine(Camera camera, Line3D m, Matrix4 transform) {
        EList<Vector3> tessellatedPoints = EList.newList();
        
        Vector3 lastViewed = null;
        
        // tessellate points
        for (var point : m.points) {
            // world transform
            Vector3 transformed = transform.multiply(point);
            Vector3 viewed = view.multiply(transformed);
            
            if (lastViewed != null) {
                Box2<Vector3, Vector3> clipped = Line3D.clipLineAgainstPlane(nearPlane, nearNorm, lastViewed, viewed);
                if (clipped != null) {
                    Vector3 a = clipped.getA();
                    Vector3 b = clipped.getB();
                    Vector3 ap = projectToScreen(a);
                    Vector3 bp = projectToScreen(b);
                    tessellatedPoints.add(ap, bp);
                }
            }
            
            lastViewed = viewed;
        }
        
        drawLine(m, tessellatedPoints);
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
    private void renderTriangles(Camera camera, Model model, Matrix4 transform) {
        EList<Triangle> tessellatedTriangles = EList.newList();
        
        // tessellate triangles
        for (var tri : model.triangles) {
            // world transform
            Triangle transformed = transform.multiply(tri);
            Vector3 normal = transformed.calculateNormal(camera.position);
            
            // only render triangles that are actually visible from the view of the camera
            float dot = transformed.cameraRay(normal, camera.position);
            if ((model.insideOut) ? dot < 0.0f : dot >= 0.0f) continue;
            
            // calcuate surface lighting
            if (!model.fullBright) {
                calculateSurfaceLighting(model, transformed, normal);
            }
            
            // transform from world space into view space and clip view triangle
            // against near plane, this could form two additional triangles
            Triangle[] clipped = view.multiply(transformed).clipAgainstPlane(nearPlane, nearNorm);
            for (int i = 0; i < clipped.length; i++) {
                // project from 3D to 2D and transform triangle position/dimensions to screen space
                tessellatedTriangles.add(projectToScreen(clipped[i], projection, offsetView, canvasWidth, canvasHeight));
            }
        }
        
        drawTriangles(tessellatedTriangles);
    }
    
    private void drawLine(Line3D line, EList<Vector3> tessellatedPoints) {
        // prime the first point
        Vector3 last = tessellatedPoints.getFirst();
        
        // if we don't even have a first point, then there's nothing to render
        if (last == null) return;
        
        int color = line.color.getRGB();
        
        for (Vector3 p : tessellatedPoints) {
            Box2<Vector3, Vector3> clipped = null;
            Vector3 ac = last; // a - clipped
            Vector3 bc = p; // b - clipped
            boolean inside = true;
            
            for (int i = 0; i < 4; i++) {
                // clip it against a plane
                switch (i) {
                case 0: clipped = Line3D.clipLineAgainstPlane(left , leftNormal , ac, bc); break;
                case 1: clipped = Line3D.clipLineAgainstPlane(bot  , botNormal  , ac, bc); break;
                case 2: clipped = Line3D.clipLineAgainstPlane(right, rightNormal, ac, bc); break;
                case 3: clipped = Line3D.clipLineAgainstPlane(top  , topNormal  , ac, bc); break;
                default: throw new IllegalStateException("This should be IMPOSSIBLE!");
                }
                
                if (clipped == null) {
                    inside = false;
                    break;
                }
                
                ac = clipped.getA();
                bc = clipped.getB();
            }
            
            last = p;
            
            if (inside) {
                if (line.antiAlias) rasterizeAALine(color, line.lineWidth, ac, bc);
                rasterizeLine(color, line.lineWidth, ac, bc);
            }
        }
    }
    
    private void drawTriangles(EList<Triangle> triangles) {
        for (Triangle t : triangles) {
            EList<Triangle> clipped = t.clipAgainstScreen(canvasWidth, canvasHeight);
            for (Triangle c : clipped) {
                if (c.texture != null) rasterizeTexturedTriangle(c);
                else rasterizeTriangle(c);
            }
        }
    }
    
    private void rasterizeLine(int baseColor, int lineWidth, Vector3 start, Vector3 end) {
        final int w = canvasWidth;
        final int h = canvasHeight;
        
        // bresenham
        for (int i = 0; i < (int) lineWidth; i++) {
            int mx = 0, my = i;
            
            int x0 = (int) start.x + mx;
            int y0 = (int) start.y + my;
            int x1 = (int) end.x + mx;
            int y1 = (int) end.y + my;
            
            int dx =  Math.abs(x1 - x0), sx = (x0 < x1) ? 1 : -1;
            int dy = -Math.abs(y1 - y0), sy = (y0 < y1) ? 1 : -1;
            int err = dx + dy, e2;
            
            while (true) {
                if (x0 == x1 && y0 == y1) break;
                if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h) {
                    float t;
                    if (dx < 10) {
                        t = ENumUtil.clamp((y0 - start.y) / (end.y - start.y), 0.0f, 1.0f);
                    }
                    else {
                        t = ENumUtil.clamp((x0 - start.x) / (end.x - start.x), 0.0f, 1.0f);
                    }
                    float depth = (end.z - start.z) * t + start.z;
                    drawPixel(x0, y0, baseColor, depth);
                }
                e2 = 2 * err;
                // step horizontally
                if (e2 > dy) { err += dy; x0 += sx; }
                // step vertically
                if (e2 < dx) { err += dx; y0 += sy; }
            }
        }
    }
    
    /** Rasterize an anti-aliased line of variable width. */
    private void rasterizeAALine(int baseColor, int lineWidth, Vector3 start, Vector3 end) {
        final int w = canvasWidth;
        final int h = canvasHeight;
        
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
            
            drawPixel(x0, y0, baseColor, depth);
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
    
    private void rasterizeTriangle(Triangle t) {
        Vector3 v0 = t.v0.pos, v1 = t.v1.pos, v2 = t.v2.pos;
        
        int minX = (int) Math.max(0, Math.ceil(Math.min(v0.x, Math.min(v1.x, v2.x))));
        int maxX = (int) Math.min(canvasWidth - 1, Math.floor(Math.max(v0.x, Math.max(v1.x, v2.x))));
        int minY = (int) Math.max(0, Math.ceil(Math.min(v0.y, Math.min(v1.y, v2.y))));
        int maxY = (int) Math.min(canvasHeight - 1, Math.floor(Math.max(v0.y, Math.max(v1.y, v2.y))));
        
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
                drawPixel(x, y, rgb, depth);
            }
        }
    }
    
    private void rasterizeTexturedTriangle(Triangle tri) {
        final BufferedImage tex = tri.texture;
        final int texWidth = tex.getWidth();
        final int texHeight = tex.getHeight();
        final float area = calcuateArea(tri);
        
        int x0 = (int) tri.v0.pos.x, y0 = (int) tri.v0.pos.y;
        int x1 = (int) tri.v1.pos.x, y1 = (int) tri.v1.pos.y;
        int x2 = (int) tri.v2.pos.x, y2 = (int) tri.v2.pos.y;
        float u0 = tri.v0.tex.x, v0 = tri.v0.tex.y, w0 = tri.v0.tex.w;
        float u1 = tri.v1.tex.x, v1 = tri.v1.tex.y, w1 = tri.v1.tex.w;
        float u2 = tri.v2.tex.x, v2 = tri.v2.tex.y, w2 = tri.v2.tex.w;

        float lighting = tri.calculatedLighting;

        // Sort vertices by y
        if (y1 < y0) {
            int t = y0; y0 = y1; y1 = t;
                t = x0; x0 = x1; x1 = t;
            float tf = u0; u0 = u1; u1 = tf;
                  tf = v0; v0 = v1; v1 = tf;
                  tf = w0; w0 = w1; w1 = tf;
        }
        if (y2 < y0) {
            int t = y0; y0 = y2; y2 = t;
                t = x0; x0 = x2; x2 = t;
            float tf = u0; u0 = u2; u2 = tf;
                  tf = v0; v0 = v2; v2 = tf;
                  tf = w0; w0 = w2; w2 = tf;
        }
        if (y2 < y1) {
            int t = y1; y1 = y2; y2 = t;
                t = x1; x1 = x2; x2 = t;
            float tf = u1; u1 = u2; u2 = tf;
                  tf = v1; v1 = v2; v2 = tf;
                  tf = w1; w1 = w2; w2 = tf;
        }

        int totalHeight = y2 - y0;
        for (int i = 0; i < totalHeight; i++) {
            boolean secondHalf = i > y1 - y0 || y1 == y0;
            int segmentHeight = secondHalf ? y2 - y1 : y1 - y0;
            float alpha = (float) i / totalHeight;
            float beta = (float) (i - (secondHalf ? y1 - y0 : 0)) / segmentHeight;

            int ax = x0 + (int) ((x2 - x0) * alpha);
            int bx = secondHalf
                ? x1 + (int) ((x2 - x1) * beta)
                : x0 + (int) ((x1 - x0) * beta);

            float su = u0 + (u2 - u0) * alpha;
            float sv = v0 + (v2 - v0) * alpha;
            float sw = w0 + (w2 - w0) * alpha;

            float eu = secondHalf
                ? u1 + (u2 - u1) * beta
                : u0 + (u1 - u0) * beta;

            float ev = secondHalf
                ? v1 + (v2 - v1) * beta
                : v0 + (v1 - v0) * beta;

            float ew = secondHalf
                ? w1 + (w2 - w1) * beta
                : w0 + (w1 - w0) * beta;

            if (ax > bx) {
                int t = ax; ax = bx; bx = t;
                float tf;
                tf = su; su = eu; eu = tf;
                tf = sv; sv = ev; ev = tf;
                tf = sw; sw = ew; ew = tf;
            }

            float step = bx != ax ? 1.0f / (bx - ax) : 0;
            float t = 0;

            int y = y0 + i;
            if (y < 0 || y >= canvasHeight) continue;

            for (int x = ax; x <= bx; x++) {
                if (x < 0 || x >= canvasWidth) continue;

                float texU = (1.0f - t) * su + t * eu;
                float texV = (1.0f - t) * sv + t * ev;
                float texW = (1.0f - t) * sw + t * ew;
                t += step;

                float invW = 1.0f / texW;

                float uu = texU * invW;
                float vv = texV * invW;
                
                // wrap to [0,1) so tiling UVs work (handles negatives too)
                uu = uu - (float) Math.floor(uu);
                vv = vv - (float) Math.floor(vv);

                // pick ONE place to flip V:
                // - If you did flip at load time (flipV = true in OBJ loader), DON'T flip here:
//                     int v = (int) (vv * texHeight);
                // - If you did NOT flip at load time, DO flip here (common in Java images):
                int u = (int) (uu * texWidth);
                int v = (int) (vv * texHeight);

                // fencepost safety (when uu or vv are exactly 1.0 due to FP rounding)
                if (u >= texWidth)  u = texWidth - 1;
                if (v >= texHeight) v = texHeight - 1;

//                // in-line clamp
//                u = (u < 0) ? 0 : (Math.min(u, texWidth - 1));
//                v = (v < 0) ? 0 : (Math.min(v, texHeight - 1));
                
                int rgb = tex.getRGB(u, v);
                int color = EColors.changeBrightness(rgb, (int) (lighting * 255));
                float depth = calculateDepth(x, y, tri, area);
                drawPixel(x, y, color, depth);
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
    private void drawPixel(int x, int y, int color, float depth) {
//        int zIndex = y * canvasWidth + x;
//        int pIndex = x + y * canvasWidth;
//        if (depth < zBuffer[zIndex]) {
//            int alpha = (color >>> 24) & 0xFF;
//
//            if (alpha < 255) {
//                int dst = pixelBuffer[pIndex];
//                if (enableBlend) {
//                    color = blend(color, dst);
//                }
//            }
//
//            pixelBuffer[pIndex] = color;
//            zBuffer[zIndex] = depth;
//        }
        final int idx = x + y * canvasWidth;
        final int zIdx = idx; // same indexing
        if (depth < zBuffer[zIdx]) {
            if (((color >>> 24) & 0xFF) != 255 && enableBlend) {
                color = blend(color, pixelBuffer[idx]);
            }
            pixelBuffer[idx] = color;
            zBuffer[zIdx] = depth;
        }
    }
    
    //==================
    // Internal Classes
    //==================
    
    private class WorkerThread implements Runnable {
        private Box2<BoxList<Entity, EList<Vector3>>, BoxList<Entity, EList<Triangle>>> toDraw;
        public WorkerThread(Box2<BoxList<Entity, EList<Vector3>>, BoxList<Entity, EList<Triangle>>> toDraw) {
            this.toDraw = toDraw;
        }
        @Override
        public void run() {
//            drawTriangles(toDraw.getB());
//            drawLines(toDraw.getA());
        }
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    public static int blend(int src, int dst) {
        int srcA = (src >>> 24);
        if (srcA == 255) return src; // fully opaque — just overwrite
        if (srcA == 0) return dst;   // fully transparent — do nothing

        int invA = 255 - srcA;

        int srcR = (src >> 16) & 0xFF;
        int srcG = (src >> 8) & 0xFF;
        int srcB = src & 0xFF;

        int dstR = (dst >> 16) & 0xFF;
        int dstG = (dst >> 8) & 0xFF;
        int dstB = dst & 0xFF;
        int dstA = (dst >>> 24);

        int outR = (srcR * srcA + dstR * invA) / 255;
        int outG = (srcG * srcA + dstG * invA) / 255;
        int outB = (srcB * srcA + dstB * invA) / 255;
        int outA = (srcA * srcA + dstA * invA) / 255;

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }
    
    private static Matrix4 makeTransform(Entity e) {
        return makeTransform(e.position, e.rotation, e.scale);
    }
    
    private static Matrix4 makeTransform(Vector3 pos, Vector3 rot, Vector3 scale) {
        Matrix4 m = new Matrix4();
        m.translate(pos);
        m.rotateXYZ(rot);
        m.scale(scale);
        return m;
    }
    
    private Triangle projectToScreen(Triangle tri, Matrix4 projection, Vector3 offsetView, int screenWidth, int screenHeight) {
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
    
    private Vector3 projectToScreen(Vector3 point) {
        Vector3 p = projection.multiply(point);
        // scale into view
        p = p.div(p.w);
        // x/y are inverted so put them back
        p.x *= -1.0f; p.y *= -1.0f;
        // offset vert into visible normalized space
        p = p.add(offsetView);
        p.x *= 0.5f * canvasWidth;
        p.y *= 0.5f * canvasHeight;
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
    
    private boolean isBackface(Triangle t) {
        Vector3 v0 = t.v0.pos;
        Vector3 v1 = t.v1.pos;
        Vector3 v2 = t.v2.pos;

        // Compute the signed area of the triangle (2D cross product)
        float area = (v1.x - v0.x) * (v2.y - v0.y) - 
                     (v2.x - v0.x) * (v1.y - v0.y);

        // If area < 0, the triangle is back-facing
        return area < 0;
    }
    
}
