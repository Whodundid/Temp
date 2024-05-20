package controller.globe;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import eutil.datatypes.points.Point2d;
import eutil.datatypes.util.EList;

/**
 * Creates a bezier line curve from the given point path.
 * <p>
 * Referenced bezier math code from:
 * https://stackoverflow.com/questions/34292504/drawing-bezier-curve-in-java
 * ~Rai
 * <p>
 * Slightly fixed math and wrapped in more convenient helper class.
 * ~Hunter Bragg
 * 
 * @author Rai
 * @author Hunter
 */
public class BezierLine {
    
    //========
    // Fields
    //========
    
    /** The starting x-position of the bezier line curve. */
    private int startX;
    /** The starting y-position of the bezier line curve. */
    private int startY;
    /** The width of the line to be drawn. */
    private float lineWidth = 1.0f;
    /** The size of each step dot (if drawn). */
    private int stepDotSize = 2;
    /** A separate color to draw step dots in (if set). */
    private Color stepDotColor;
    /** The complete list of all points included in this bezier line. */
    private final EList<Point2d> points = EList.newList();
    /** Debug flag used to specify whether or not point locations will be drawn. */
    private boolean drawControlPoints = false;
    /** Debug flag used to specfiy whether or not point coordinate strings will be drawn. */
    private boolean drawControlPointCoordinates = false;
    /** Debug flag used to specify whether or not each t-step iteration's x/y coordinate dot will be drawn. */
    private boolean drawStepDots = false;
    /** Specifies whether or not the line will actually be drawn. */
    private boolean drawLine = true;
    
    //==============
    // Constructors
    //==============
    
    public BezierLine(Point2d... points) {
        if (points == null) return;
        
        if (points != null && points.length > 0) {
            this.startX = (int) points[0].x;
            this.startY = (int) points[0].y;
        }
        
        this.points.addA(points);
    }
    
    public BezierLine(int startX, int startY) {
        this(startX, startY, new Point2d[0]);
    }
    
    public BezierLine(int startX, int startY, Point2d... points) {
        this.startX = startX;
        this.startY = startY;
        if (points != null) this.points.addA(points);
    }
    
    //=========
    // Methods
    //=========
    
    /** Appends a new point onto the end of this line. */
    public void addPoint(double x, double y) { addPoint(new Point2d(x, y)); }
    /** Appends a new point onto the end of this line. */
    public void addPoint(Point2d point) { points.add(point); }
    
    /**
     * Generates 20 curve step points based on the start, end and all intermediate
     * control points for this bezier curve line.
     * 
     * @return EList<Point2d> Curve step points for this bezier curve
     */
    public EList<Point2d> generateCurvePoints() {
        return generateCurvePoints(20);
    }
    
    /**
     * Generates a specified number of curve step points based on the start, end
     * and all intermediate control points for this bezier curve line.
     * 
     * @param  iterations The number of step points to create (higher => more detail)
     * 
     * @return            EList<Point2d> Curve step points for this bezier curve
     */
    public EList<Point2d> generateCurvePoints(int iterations) {
        EList<Point2d> r = EList.newList();
        final float itF = iterations;
        for (int t = 0; t < iterations; t++) {
            Point2d p = besierCurvePixel(t / itF);
            p.x += startX;
            p.y += startY;
            r.add(p);
        }
        if (points.size() >= 1) {
            r.add(points.getLast());
        }
        return r;
    }
    
    /**
     * Generates and draws 20 iterations worth of bezier line points.
     * 
     * @param g2d The AWT graphics instance to draw to
     */
    public void draw(Graphics2D g2d) {
        draw(g2d, 20);
    }
    
    /**
     * Generates a specified number of iterations worth of bezier line points.
     * 
     * @param g2d The AWT graphics instance to draw to
     * @param iterations The number of step points to create (higher => more detail)
     */
    public void draw(Graphics2D g2d, int iterations) {
        if (points.isNotEmpty()) {
            final EList<Point2d> generatedPoints = generateCurvePoints(iterations);
            drawLine(g2d, generatedPoints);
        }
        
        if (drawControlPoints) {
            for (int i = 0; i < points.size(); i++) {
                Point2d p = points.get(i);
                g2d.setColor(Color.WHITE);
                g2d.drawOval((int) p.x - 3, (int) p.y - 3, 6, 6);
            }
        }
        
        if (drawControlPointCoordinates) {
            for (int i = 0; i < points.size(); i++) {
                Point2d p = points.get(i);
                String out = i + ": " + p;
                int len = g2d.getFontMetrics().stringWidth(out);
                g2d.drawString(out, (int) p.x - 3 - len / 2, (int) p.y - 10);
            }
        }
    }
    
    //=========
    // Getters
    //=========
    
    public Point2d getStartPos() { return new Point2d(startX, startY); }
    public EList<Point2d> getPoints() { return points.copy(); }
    public boolean areControlPointsDrawn() { return drawControlPoints; }
    public boolean areControlPointCoordinatesDrawn() { return drawControlPointCoordinates; }
    public boolean areStepDotsDrawn() { return drawStepDots; }
    public float getLineWidth() { return lineWidth; }
    public boolean isLineDrawn() { return drawLine; }
    public int getStepDotSize() { return stepDotSize; }
    public Color getStepDotColor() { return stepDotColor; }
    
    //=========
    // Setters
    //=========
    
    public void setDrawControlPoints(boolean val) { drawControlPoints = val; }
    public void setDrawControlPointCoordinates(boolean val) { drawControlPointCoordinates = val; }
    public void setDrawStepDots(boolean val) { drawStepDots = val; }
    public void setLineWidth(float width) { lineWidth = width; }
    public void setLineDrawn(boolean val) { drawLine = val; }
    public void setStepDotSize(int size) { stepDotSize = size; }
    public void setStepDotColor(Color color) { stepDotColor = color; }
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    /**
     * Performs bezier curve math to calculate intermediate step line
     * coordinates based on given step 't'.
     */
    private Point2d besierCurvePixel(float t) {
        double bPoly[] = new double[points.size() + 1];
        
        for (int i = 0; i <= points.size(); i++) {
            bPoly[i] = bernstein(t, points.size() - 1, i);
        }
        
        double sumX = 0.0, sumY = 0.0;
        for (int i = 1; i < points.size(); i++) {
            sumX += bPoly[i] * (points.get(i).x - startX);
            sumY += bPoly[i] * (points.get(i).y - startY);
        }
        
        int x = (int) Math.round(sumX);
        int y = (int) Math.round(sumY);
        
        return new Point2d(x, y);
    }
    
    /**
     * Method to actually perform line drawing.
     * 
     * @param g2d The AWT graphics instance to draw to
     * @param points The set of curve line points to draw
     */
    protected void drawLine(Graphics2D g2d, EList<Point2d> points) {
        var oldStroke = g2d.getStroke();
        
        try {
            BasicStroke stroke = new BasicStroke(lineWidth);
            g2d.setStroke(stroke);
            Point2d last = null;
            
            // draw connecting line
            if (drawLine) {
                for (Point2d p : points) {
                    if (last != null) {
                        g2d.drawLine((int) last.x, (int) last.y, (int) p.x, (int) p.y);
                    }
                    last = p;
                }
            }
            
            // draw debug step dots
            if (drawStepDots) {
                for (Point2d p : points) {
                    Color c = g2d.getColor();
                    if (stepDotColor != null) g2d.setColor(stepDotColor);
                    g2d.fillOval((int) p.x - stepDotSize / 2, (int) p.y - stepDotSize / 2, stepDotSize, stepDotSize);
                    if (stepDotColor != null) g2d.setColor(c);
                    last = p;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // regardless of success, always restore the original stroke
            g2d.setStroke(oldStroke);
        }
    }
    
    //=======================
    // Static Helper Methods
    //=======================
    
    /**
     * https://stackoverflow.com/questions/34292504/drawing-bezier-curve-in-java
     * @author Rai
     */
    private static int factorial(int n) {
        int factorial = 1;
        for (int i = 1; i <= n; i++) {
            factorial *= i;
        }
        return factorial;
    }
    
    /**
     * https://stackoverflow.com/questions/34292504/drawing-bezier-curve-in-java
     * @author Rai
     */
    private static double bernstein(float t, int n, int i) {
        return (factorial(n) / (factorial(i) * factorial(n - i))) * Math.pow(1 - t, n - i) * Math.pow(t, i);
    }
    
}
