package controller.globe;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JPanel;

import eutil.colors.EColors;
import eutil.datatypes.util.EList;
import eutil.math.ENumUtil;
import eutil.random.ERandomUtil;

public class RenderingPanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    
    //========
    // Fields
    //========
    
    private Camera camera;
    private PerspectiveRenderer renderer;
    private BufferedImage img;
    
    private EList<Entity> entities = EList.newList();
    
    private boolean lockCursor = false;
    private Robot mouseMover;
    private Cursor standard;
    private Cursor hidden;
    
    private int renderScale = 50;
    private int imgWidth = 320;
    private int imgHeight = 240;
    
    private int lastX, lastY;
    private volatile boolean leftPress = false;
    
    Entity earth;
    Entity cube1, cube2;
    Entity t1;
    Entity line1;
    
    private long tps = 60;
    private double timeT = 1000.0 / tps;
    private double deltaT = 0;
    private int curNumTicks = 0;
    private int ticks = 0;
    
    private long fps = 60;
    private double timeF = 1000.0 / fps;
    private double deltaF = 0;
    private long startTime = 0L;
    private long curTime = 0L;
    private long oldTime = 0L;
    private float dt = 0.0f;
    private long timer;
    private long initialTime = 0L;
    private long runningTime = 0L;
    private int frames = 0;
    private int curFrameRate = 0;
    
    private boolean preventMovement = false;
    private long timeOfLastKeyPress;
    private boolean gDown;
    private boolean rDown;
    private boolean yDown;
    private boolean cDown;
    private boolean oDown;
    
    private double fretboardYPos = 0;
    private int fretboardSectionHeight = 75;
    private long songStartTime;
    private long songPlayingTime;
    private NoteSong activeSong;
    private long noteHitWindow = 700L;
    private int combo;
    private int multiplier = 1;
    private int pointsForNote = 50;
    private int score;
    private long lastMissTime;
    private int rockMeter;
    private boolean rockMeterFlashing = false;
    private long lastRockMeterFlash;
    private long rockMeterFlashDuration;
    
    private class NoteSong {
        public NoteTimings green, red, yellow, cyan, orange;
        public long duration;
        
        public NoteSong() {
            green = new NoteTimings();
            red = new NoteTimings();
            yellow = new NoteTimings();
            cyan = new NoteTimings();
            orange = new NoteTimings();
        }
        
        public NoteSong(File songFile) {
            
        }
        
        public void updateSongNotes() {
            if (green != null) updateNotes(green);
            if (red != null) updateNotes(red);
            if (yellow != null) updateNotes(yellow);
            if (cyan != null) updateNotes(cyan);
            if (orange != null) updateNotes(orange);
        }
        
        void updateNotes(NoteTimings notes) {
            if (!notes.hasMoreNotes()) return;
            long nextGreenNoteTime = notes.timeOfNextNote;
            if (nextGreenNoteTime < songPlayingTime) {
                notes.advanceNote();
            }
        }
    }
    
    private class NoteTimings {
        long timeOfNextNote;
        int currentNoteIndex = -1;
        long[] noteTimes;
        boolean[] notesHit;
        
        public void setNotes(int notes) {
            noteTimes = new long[notes];
            notesHit = new boolean[notes];
        }
        
        public void advanceNote() {
            if (noteTimes == null) return;
            currentNoteIndex++;
            if (currentNoteIndex < noteTimes.length) {
                timeOfNextNote = noteTimes[currentNoteIndex];
            }
        }
        
        public boolean hasMoreNotes() {
            if (noteTimes == null) return false;
            return currentNoteIndex < noteTimes.length;
        }
    }
    
    //==============
    // Constructors
    //==============
    
    public RenderingPanel(int width, int height) {
        imgWidth = width;
        imgHeight = height;
        
        camera = new Camera();
        renderer = new PerspectiveRenderer(width, height);
        
        // cursor stuff
        standard = getCursor();
        BufferedImage hiddenCursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        hidden = Toolkit.getDefaultToolkit().createCustomCursor(hiddenCursor, new Point(0, 0), "hidden");
        
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        
        try {
            mouseMover = new Robot();
        }
        catch (AWTException e) {
            e.printStackTrace();
        }
        
        // prepare timers
        startTime = System.currentTimeMillis();
        oldTime = startTime;
        timer = startTime;
        initialTime = System.currentTimeMillis();
        
        new Thread(() -> {
            while (true) {
                thing();
            }
        }).start();
    }
    
    private void thing() {
        try {
            long currentTime = System.currentTimeMillis();
            deltaT += (currentTime - initialTime) / timeT;
            deltaF += (currentTime - initialTime) / timeF;
            initialTime = currentTime;
            
            if (deltaT >= 1) {
                oldTime = curTime;
                curTime = System.currentTimeMillis();
                
                // 'dt' is ms
                dt = curTime - oldTime;
                //if (dt > 15.0f) dt = 15.0f;
                ticks++;
                runTick(dt);
                
                deltaT--;
            }
            
            if (deltaF >= 1) {
                runRenderTick(currentTime - oldTime);
                frames++;
                deltaF--;
            }
            
            // measure fps
            if (currentTime - timer > 1000) {
                curFrameRate = frames;
                curNumTicks = ticks;
                frames = 0;
                ticks = 0;
                timer += 1000;
            }
            
            if (deltaT > 3 || deltaF > 5) {
                deltaT = 0;
                deltaF = 0;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void runTick(float dt) {
        if (t1 != null) t1.rotation.y += 0.001f;
        if (earth != null && !leftPress) earth.rotation.y += 0.0001f;
        if (line1 != null && !leftPress) line1.rotation.y += 0.0001f;
        if (cube1 != null) {
            cube1.rotation.x += 0.01f;
            cube1.rotation.y += 0.001f;
            cube1.rotation.z += 0.005f;
        }
        if (cube2 != null) cube2.rotation.y -= 0.01f;
        
        if (activeSong != null) {
            songPlayingTime += dt;
            if (songPlayingTime >= activeSong.duration || rockMeter == 0) {
                activeSong = null;
            }
            else {
                activeSong.updateSongNotes();
            }
        }
        
        fretboardYPos += (0.121 * dt);
        if (fretboardYPos >= fretboardSectionHeight) {
            fretboardYPos = 0;
        }
    }
    
    private void runRenderTick(long dt) {
        repaint();
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        
        // draw black background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // enable anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // setup image and render
        createPanelImage();
        renderer.render(camera, entities, img);
        
        // draw debug camera position and rotation
        g2.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, imgWidth, imgHeight, null);
        
        try {
            drawBanjo(g2);
        }
        catch (Exception e) {
            System.out.println("bad");
        }
        
        g2.setColor(Color.WHITE);
        String pos = "POS: " + camera.position;
        var posGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), pos);
        g2.drawString(pos, 0, (int) posGV.getVisualBounds().getHeight());
        String rot = "ROT: " + camera.rotation;
        var rotGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), rot);
        g2.drawString(rot, 0, (int) rotGV.getVisualBounds().getHeight() * 2);
        String fov = "FOV: " + renderer.getFOV();
        var fovGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), fov);
        g2.drawString(fov, 0, (int) fovGV.getVisualBounds().getHeight() * 4);
        String quality = "Q: " + renderScale;
        var qGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), quality);
        g2.drawString(quality, 0, (int) qGV.getVisualBounds().getHeight() * 5);
        String fps = "FPS: " + curFrameRate;
        var fpsGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), fps);
        g2.drawString(fps, 0.0f, (float) fpsGV.getVisualBounds().getHeight() * 6.5f);
        String tps = "TPS: " + curNumTicks;
        var tpsGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), tps);
        g2.drawString(tps, 0.0f, (float) tpsGV.getVisualBounds().getHeight() * 8f);
        
        // draw crosshair
        int midX = getWidth() / 2;
        int midY = getHeight() / 2;
        g2.setColor(Color.RED);
        g2.fillRect(midX - 4, midY, 10, 2);
        g2.fillRect(midX, midY - 4, 2, 10);
        
//        g2.setColor(Color.RED);
//        Point2d a = new Point2d(400, 300);
//        Point2d b = new Point2d(500, 200);
//        Point2d c = new Point2d(600, 100);
//        Point2d d = new Point2d(700, 200);
//        Point2d e = new Point2d(800, 300);
//        Point2d f = new Point2d(900, 400);
//        Point2d h = new Point2d(1000, 500);
//        Point2d i = new Point2d(1100, 400);
//        Point2d j = new Point2d(1200, 300);
//        var line = new BezierLine(a, b, c, d, e, f, h, i, j);
//        line.setDrawControlPoints(true);
//        line.setDrawStepDots(true);
//        line.setStepDotColor(Color.cyan);
//        line.setStepDotSize(5);
//        line.setLineWidth(2);
//        line.draw(g2, 80);
    }
    
    @SuppressWarnings("unused")
    public void drawBanjo(Graphics2D g2) {
        preventMovement = true;
        
        Color noteBackground = Color.DARK_GRAY;
        
        Color gColor = (gDown) ? Color.GREEN : noteBackground;
        Color rColor = (rDown) ? Color.RED : noteBackground;
        Color yColor = (yDown) ? Color.YELLOW : noteBackground;
        Color cColor = (cDown) ? Color.CYAN : noteBackground;
        Color oColor = (oDown) ? Color.ORANGE : noteBackground;
        
        Color gBorder = Color.GREEN.darker();
        Color rBorder = Color.RED.darker();
        Color yBorder = Color.YELLOW.darker();
        Color cBorder = Color.CYAN.darker();
        Color oBorder = Color.ORANGE.darker();
        
        int noteWidth = 50;
        int noteHeight = 40;
        int outlineSize = 4;
        int outlineWidth = noteWidth + outlineSize;
        int outlineHeight = noteHeight + outlineSize;
        int noteGap = 7;
        int noteGapFromBottom = 3;
        int numNotes = 5;
        
        int startX = 0;
        int startY = 0;
        int width = getWidth();
        int height = getHeight();
        int midX = startX + width / 2;
        int midY = startY + height / 2;
        int endX = startX + width;
        int endY = startY + height;
        
        int fullNotesWidth = (numNotes * outlineWidth + (numNotes - 1) * noteGap);
        int noteStartX = midX - fullNotesWidth / 2;
        int noteStartY = height - noteGapFromBottom - outlineHeight;
        
        int fretboardStartX = noteStartX - noteGap;
        int fretboardEndX = fretboardStartX + fullNotesWidth + noteGap * 2;
        int fretboardStartY = height - (int) (height * 0.60);
        int fretboardEndY = height;
        int fretboardWidth = fretboardEndX - fretboardStartX;
        int fretboardHeight = fretboardEndY - fretboardStartY;
        
        int fullOpacity = fretboardStartY + height / 4;
        double fullOpacityD = fullOpacity;
        double opacityRange = (fullOpacityD - fretboardStartY);
        //Color fretboardColor = Color.decode("#b2a38b");
        Color fretboardColor = Color.DARK_GRAY.darker();
        Color fretboardBorderColor = Color.LIGHT_GRAY;
        final int fretboardRGB = fretboardColor.getRGB();
        final int freboardBorderRGB = fretboardBorderColor.getRGB();
        
        // draw fret board
        for (int y = fretboardStartY; y < fullOpacity; y++) {
            double yy = y;
            int opacity = (int) (((yy - fullOpacityD) / opacityRange) * 255.0);
            if (opacity == 0) opacity = 255;
            g2.setColor(new Color(EColors.changeOpacity(fretboardRGB, opacity), true));
            g2.drawRect(fretboardStartX, y, fretboardWidth, 1);
            g2.setColor(new Color(EColors.changeOpacity(freboardBorderRGB, opacity), true));
            g2.fillRect(fretboardStartX - 2, y, 2, 1);
            g2.fillRect(fretboardEndX, y, 2, 1);
        }
        g2.setColor(fretboardColor);
        g2.fillRect(fretboardStartX, fullOpacity, fretboardWidth, fretboardEndY - fullOpacity);
        g2.setColor(fretboardBorderColor);
        g2.fillRect(fretboardStartX - 2, fullOpacity, 2, fretboardEndY - fullOpacity);
        g2.fillRect(fretboardEndX, fullOpacity, 2, fretboardEndY - fullOpacity);
        
        // draw fretboard sections
        int numFretboardSections = (fretboardHeight / fretboardSectionHeight) + 1;
        int fretboardSectionStartY = (int) (fretboardStartY + fretboardYPos);
        int fretboardSectionDividerHeight = 2;
        Color fretboardSectionDividerColor = Color.LIGHT_GRAY;
        if (System.currentTimeMillis() - lastMissTime <= 50) fretboardSectionDividerColor = Color.RED;
        final int fretboardSectionDividerRGB = fretboardSectionDividerColor.getRGB();
        
        for (int i = 0; i < numFretboardSections; i++) {
            int x = fretboardStartX;
            int y = fretboardSectionStartY + (i * fretboardSectionHeight);
            Color c = fretboardSectionDividerColor;
            if (y < fullOpacity) {
                double yy = y;
                int opacity = (int) (((yy - fullOpacityD) / opacityRange) * 255.0);
                if (opacity == 0) opacity = 255;
                int rgb = EColors.changeOpacity(fretboardSectionDividerRGB, opacity);
                c = new Color(rgb, true);
            }
            g2.setColor(c);
            g2.fillRect(x, y, fretboardWidth, fretboardSectionDividerHeight);
        }
        
        // draw controllable notes on fret board
        for (int i = 0; i < numNotes; i++) {
            Color borderColor, noteColor;
            
            switch (i) {
            case 0: borderColor = gBorder; noteColor = gColor; break;
            case 1: borderColor = rBorder; noteColor = rColor; break;
            case 2: borderColor = yBorder; noteColor = yColor; break;
            case 3: borderColor = cBorder; noteColor = cColor; break;
            case 4: borderColor = oBorder; noteColor = oColor; break;
            default: borderColor = Color.DARK_GRAY.darker(); noteColor = Color.DARK_GRAY;
            }
            
            int x = noteStartX + (i * outlineWidth);
            int y = noteStartY;
            
            if (i >= 1) x += (i * noteGap);
            
            g2.setColor(borderColor);
            g2.fillOval(x, y, outlineWidth, outlineHeight);
            g2.setColor(noteColor);
            g2.fillOval(x + outlineSize, y + outlineSize, noteWidth - outlineSize, noteHeight - outlineSize);
        }
        
        int stringWidth = 2;
        int noteStringStartX = noteStartX + outlineWidth / 2 - (stringWidth / 2);
        int stringHeight = noteStartY - fretboardStartY;
        
        // draw fretboard note strings
        for (int i = 0; i < numNotes; i++) {
            int x = noteStringStartX + (i * outlineWidth);
            int y = fretboardStartY;
            if (i >= 1) x += (i * noteGap);
            
            for (; y < (fretboardStartY + stringHeight); y++) {
                Color c = Color.BLACK;
                if (y < fullOpacity) {
                    double yy = y;
                    int opacity = (int) (((yy - fullOpacityD) / opacityRange) * 255.0);
                    if (opacity == 0) opacity = 255;
                    int rgb = EColors.changeOpacity(c.getRGB(), opacity);
                    c = new Color(rgb, true);
                }
                g2.setColor(c);
                g2.fillRect(x, y, stringWidth, 1);
            }
        }
        
        // determine veiwable notes
        double viewableNotesDuration = 5000L; // 5 seconds
        if (activeSong != null) {
            var positions = new Positions(outlineSize, viewableNotesDuration, fretboardHeight, fretboardStartY,
                                          fullOpacity, fullOpacityD, opacityRange, noteWidth, noteHeight);
            int greenX = noteStartX;
            int redX = greenX + outlineWidth + noteGap;
            int yellowX = redX + outlineWidth + noteGap;
            int cyanX = yellowX + outlineWidth + noteGap;
            int orangeX = cyanX + outlineWidth + noteGap;
            drawNotes(greenX, positions, activeSong.green, Color.GREEN, g2);
            drawNotes(redX, positions, activeSong.red, Color.RED, g2);
            drawNotes(yellowX, positions, activeSong.yellow, Color.YELLOW, g2);
            drawNotes(cyanX, positions, activeSong.cyan, Color.CYAN, g2);
            drawNotes(orangeX, positions, activeSong.orange, Color.ORANGE, g2);
        }
        
        // draw score, multiplier and combo
        int scoreBoxStartX = fretboardEndX + (fretboardWidth / 8);
        int scoreBoxStartY = fretboardStartY + (fretboardHeight / 2) - fretboardHeight / 4;
        int scoreBoxWidth = fretboardWidth / 2;
        int scoreBoxHeight = fretboardHeight / 12;
        Font scoreFont = new Font("Seril", Font.BOLD, scoreBoxHeight - 4);
        // preserve the current font
        var font = g2.getFont();
        
        // draw score
        g2.setFont(scoreFont);
        g2.setColor(Color.GRAY);
        g2.fillRect(scoreBoxStartX, scoreBoxStartY, scoreBoxWidth, scoreBoxHeight);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(scoreBoxStartX, scoreBoxStartY, scoreBoxWidth, scoreBoxHeight);
        g2.setColor(Color.WHITE);
        String score = "" + this.score;
        var scoreGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), score);
        g2.drawString(score, scoreBoxStartX + 5, (float) (scoreBoxStartY + (scoreBoxHeight / 2) + scoreGV.getVisualBounds().getHeight() / 2));
        
        // draw multiplier
        int multiplierBoxStartY = scoreBoxStartY + scoreBoxHeight;
        int multiplierBoxHeight = fretboardHeight / 6;
        Font multiplierFont = new Font("Seril", Font.BOLD, multiplierBoxHeight - 20);
        g2.setFont(multiplierFont);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(scoreBoxStartX, multiplierBoxStartY, scoreBoxWidth, multiplierBoxHeight);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(scoreBoxStartX, multiplierBoxStartY, scoreBoxWidth, multiplierBoxHeight);
        String multiplierString = multiplier + "x";
        var multiplierGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), multiplierString);
        Color multiplierColor = switch (multiplier) {
        case 1 -> Color.WHITE;
        case 2 -> Color.ORANGE;
        case 3 -> Color.GREEN;
        case 4 -> Color.MAGENTA.darker();
        default -> Color.WHITE;
        };
        g2.setColor(multiplierColor);
        g2.drawString(multiplierString, scoreBoxStartX + 10, (float) (multiplierBoxStartY + (multiplierBoxHeight / 2) + multiplierGV.getVisualBounds().getHeight() / 2));
        
        // draw combo
        int comboBoxStartY = multiplierBoxStartY + multiplierBoxHeight;
        int comboBoxHeight = fretboardHeight / 16;
        Font comboFont = new Font("Seril", Font.BOLD, comboBoxHeight - 10);
        g2.setFont(comboFont);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(scoreBoxStartX, comboBoxStartY, scoreBoxWidth, comboBoxHeight);
        String comboString = "" + combo;
        var comboGV = g2.getFont().createGlyphVector(g2.getFontRenderContext(), comboString);
        g2.setColor(Color.GREEN.darker());
        g2.drawString(comboString, scoreBoxStartX + 10, (float) (comboBoxStartY + (comboBoxHeight / 2) + comboGV.getVisualBounds().getHeight() / 2));
        
        // restore the original font
        g2.setFont(font);
        
        // draw rock meter
        int rockMeterWidth = fretboardWidth / 12;
        int rockMeterHeight = (int) (fretboardHeight * 0.9);
        int rockMeterStartX = fretboardStartX - rockMeterWidth - (fretboardWidth / 8);
        int rockMeterStartY = fretboardStartY + (fretboardHeight / 2) - (rockMeterHeight / 2);
        
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(rockMeterStartX, rockMeterStartY, rockMeterWidth, rockMeterHeight);
        g2.setColor(Color.GRAY);
        g2.drawRect(rockMeterStartX, rockMeterStartY, rockMeterWidth, rockMeterHeight);
        
        int rockMeterXPos = rockMeterStartX + (rockMeterWidth / 6);
        int rockMeterRatio = (int) (rockMeterHeight - ((double) rockMeter / 99.0) * (rockMeterHeight - ((double) rockMeterHeight / 64.0)));
        int rockMeterYPos = rockMeterStartY + (rockMeterHeight / 128) + rockMeterRatio - 2;
        int rockMeterYHeight = (rockMeterStartY + rockMeterHeight - (rockMeterHeight / 128)) - rockMeterYPos;
        int rockMeterXWidth = (rockMeterStartX + rockMeterWidth) - (rockMeterWidth / 6) - rockMeterXPos;
        Color rockMeterColor = Color.YELLOW;
        if (rockMeter >= 75) rockMeterColor = Color.GREEN;
        if (rockMeter <= 35) {
            if (curTime - lastRockMeterFlash >= 300) {
                rockMeterFlashing = !rockMeterFlashing;
                lastRockMeterFlash = curTime;
            }
            rockMeterColor = (rockMeterFlashing) ? Color.decode("#ff8989") : Color.RED;
        }
        g2.setColor(rockMeterColor);
        g2.fillRect(rockMeterXPos, rockMeterYPos, rockMeterXWidth, rockMeterYHeight);
    }
    
    private class Positions {
        int outlineSize;
        double viewableNotesDuration;
        int fretboardHeight;
        int fretboardStartY;
        int fullOpacity;
        double fullOpacityD;
        double opacityRange;
        int noteWidth;
        int noteHeight;
        public Positions(int b, double c, int d, int e, int f, double g, double h, int i, int j) {
            outlineSize = b;
            viewableNotesDuration = c;
            fretboardHeight = d;
            fretboardStartY = e;
            fullOpacity = f;
            fullOpacityD = g;
            opacityRange = h;
            noteWidth = i;
            noteHeight = j;
        }
    }
    
    private void drawNotes(int noteStartX, Positions p, NoteTimings notes, Color color, Graphics2D g2) {
        if (!notes.hasMoreNotes()) return;
        double currentSongTimeWindow = songPlayingTime + p.viewableNotesDuration;
        int noteIndex = notes.currentNoteIndex;
        long[] times = notes.noteTimes;
        int x = noteStartX + p.outlineSize;
        for (int i = noteIndex; i < times.length; i++) {
            Color c = color.darker().darker();
            long noteTime = times[i];
            if (noteTime > currentSongTimeWindow) break;
            double timeTillNextNote = noteTime - songPlayingTime;
            // if a note wasn't hit in time, then it was missed
            if (timeTillNextNote < 200 && !notes.notesHit[i]) resetCombo();
            double ratio = (timeTillNextNote / p.viewableNotesDuration);
            double yPos = p.fretboardHeight - (ratio * p.fretboardHeight);
            int y = (int) (p.fretboardStartY + yPos);
            if (y < p.fullOpacity) {
                double yy = y;
                int opacity = (int) (((yy - p.fullOpacityD) / p.opacityRange) * 255.0);
                if (opacity == 0) opacity = 255;
                int rgb = EColors.changeOpacity(c.getRGB(), opacity);
                c = new Color(rgb, true);
            }
            g2.setColor(c);
            g2.fillOval(x, y, p.noteWidth - p.outlineSize, p.noteHeight - p.outlineSize);
            g2.setColor(Color.BLACK);
            g2.drawOval(x, y, p.noteWidth - p.outlineSize, p.noteHeight - p.outlineSize);
        }
    }
    
    private void strum() {
        if (activeSong == null) return;
        if (gDown) strumNotes(activeSong.green);
        if (rDown) strumNotes(activeSong.red);
        if (yDown) strumNotes(activeSong.yellow);
        if (cDown) strumNotes(activeSong.cyan);
        if (oDown) strumNotes(activeSong.orange);
        
        // strum was made but no notes were pressed
        if (!gDown && !rDown && !yDown && !cDown && !oDown) {
            resetCombo();
        }
    }
    
    private void strumNotes(NoteTimings notes) {
        if (!notes.hasMoreNotes()) {
            resetCombo();
            return;
        }
        
        long noteTime = notes.timeOfNextNote;
        double timeTillNextNote = noteTime - songPlayingTime;
        
        // hit an already hit note
        if (notes.notesHit[notes.currentNoteIndex]) {
            resetCombo();
        }
        // is not hit and is within window
        else if (timeTillNextNote <= noteHitWindow) {
            notes.notesHit[notes.currentNoteIndex] = true;
            notes.advanceNote();
            hitNote();
        }
        // miss
        else {
            resetCombo();
        }
    }
    
    private void hitNote() {
        combo++;
        if (combo < 10) multiplier = 1;
        else if (combo >= 10 && combo < 20) multiplier = 2;
        else if (combo >= 20 && combo < 30) multiplier = 3;
        else if (combo >= 30) multiplier = 4;
        score += pointsForNote * multiplier;
        rockMeter = ENumUtil.clamp(rockMeter + multiplier, 0, 100);
    }
    
    private void resetCombo() {
        combo = 0;
        multiplier = 1;
        rockMeter = ENumUtil.clamp(rockMeter - 1, 0, 100);
        lastMissTime = System.currentTimeMillis();
    }
    
    private void startSong() {
        var song = new NoteSong();
        
        song.duration = 90000L;
        int greenNotes = ERandomUtil.getRoll(50, 70);
        int redNotes = ERandomUtil.getRoll(30, 70);
        int yellowNotes = ERandomUtil.getRoll(40, 70);
        int cyanNotes = ERandomUtil.getRoll(25, 70);
        int orangeNotes = ERandomUtil.getRoll(25, 35);
        
        song.green.setNotes(greenNotes);
        song.red.setNotes(redNotes);
        song.yellow.setNotes(yellowNotes);
        song.cyan.setNotes(cyanNotes);
        song.orange.setNotes(orangeNotes);
        
        long startTime = 10000L;
        long previousGreenTime = startTime;
        long previousRedTime = startTime;
        long previousYellowTime = startTime;
        long previousCyanTime = startTime;
        long previousOrangeTime = startTime;
        
        for (int i = 0; i < greenNotes; i++) {
            previousGreenTime = song.green.noteTimes[i] = (previousGreenTime + ERandomUtil.getRoll(500, 3600));
        }
        for (int i = 0; i < redNotes; i++) {
            previousRedTime = song.red.noteTimes[i] = (previousRedTime + ERandomUtil.getRoll(500, 3500));
        }
        for (int i = 0; i < yellowNotes; i++) {
            previousYellowTime = song.yellow.noteTimes[i] = (previousYellowTime + ERandomUtil.getRoll(500, 3800));
        }
        for (int i = 0; i < cyanNotes; i++) {
            previousCyanTime = song.cyan.noteTimes[i] = (previousCyanTime + ERandomUtil.getRoll(500, 3000));
        }
        for (int i = 0; i < orangeNotes; i++) {
            previousOrangeTime = song.orange.noteTimes[i] = (previousOrangeTime + ERandomUtil.getRoll(500, 4500));
        }
        
        activeSong = song;
        songStartTime = System.currentTimeMillis();
        songPlayingTime = 0L;
        score = 0;
        multiplier = 1;
        combo = 0;
        rockMeter = 50;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (!preventMovement) {
            camera.onKeyPressed(e);
        }
        
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_T) {
            if (lockCursor) lockCursor(false);
        }
        
        int key = e.getKeyCode();
        long time = 0L;
        switch (key) {
        case KeyEvent.VK_A: gDown = true; time = System.currentTimeMillis(); break;
        case KeyEvent.VK_S: rDown = true; time = System.currentTimeMillis(); break;
        case KeyEvent.VK_D: yDown = true; time = System.currentTimeMillis(); break;
        case KeyEvent.VK_F: cDown = true; time = System.currentTimeMillis(); break;
        case KeyEvent.VK_G: oDown = true; time = System.currentTimeMillis(); break;
        case KeyEvent.VK_UP: strum(); break;
        case KeyEvent.VK_DOWN: strum(); break;
        case KeyEvent.VK_Q: startSong(); break;
        default: break;
        }
        timeOfLastKeyPress = time;
        
        
        repaint();
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
        case KeyEvent.VK_A: gDown = false; break;
        case KeyEvent.VK_S: rDown = false; break;
        case KeyEvent.VK_D: yDown = false; break;
        case KeyEvent.VK_F: cDown = false; break;
        case KeyEvent.VK_G: oDown = false; break;
        default: break;
        }
        
        repaint();
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!lockCursor) return;
        
        float dx = e.getX() - getWidth() / 2;
        float dy = e.getY() - getHeight() / 2;
        
        var loc = getLocationOnScreen();
        mouseMover.mouseMove(loc.x + getWidth() / 2,
                             loc.y + getHeight() / 2);
        
        dx /= 180.0;
        dy /= -180.0;
        
        float fovRatio = renderer.getFOV() / 120f;
        dx *= fovRatio;
        dy *= fovRatio;
        
        camera.updateLook(dy * 15f, dx * 15f, 0);
        repaint();
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
            renderScale += e.getWheelRotation() * -1;
            renderScale = ENumUtil.clamp(renderScale, 1, 120);
            setup();
        }
        else {
            float scale = 1.00f;
            if (e.isAltDown()) scale = 0.1f;
            float fov = renderer.getFOV() + e.getWheelRotation() * scale;
            fov = ENumUtil.clamp(fov, 0.1f, 120f);
            renderer.setFOV(fov);
        }

        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == 1) {
            lockCursor(true);
        }
        if (e.getButton() == 2) {
            camera.reset();
            repaint();
        }
        if (e.getButton() == 3) {
            lastX = e.getX();
            lastY = e.getY();
            leftPress = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == 3) leftPress = false;
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (leftPress) {
            int x = e.getX();
            int y = e.getY();
            
            float dx = x - lastX;
            float dy = y - lastY;
            
            float amount = 0.003f;
            dx *= amount;
            dy *= amount;
            
//            float y$1 = (float) Math.sin(Math.toRadians(earth.rotation.x));
//            float y$2 = (float) Math.cos(Math.toRadians(earth.rotation.x));
//            float my = y$1 * amount;
//            float mx = (float) Math.sin(Math.toRadians(earth.rotation.y)) * amount;
//            float mz = (float) Math.cos(Math.toRadians(earth.rotation.y)) * amount;
//            float wsx = mx * y$2;
//            float wsz = mz * y$2;
            
            System.out.println(earth.rotation.y * 180 / Math.PI);
            
//            dx /= 180.0;
//            dy /= -180.0;
            
//            float fovRatio = renderer.getFOV() / 120f;
//            dx *= fovRatio;
//            dy *= fovRatio;
            
            earth.rotation.addT(0, -dx, 0);
            line1.rotation.addT(0, -dx, 0);
            
            lastX = x;
            lastY = y;
        }
    }
    
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    
    //=========
    // Methods
    //=========
    
    public void lockCursor(boolean val) {
        lockCursor = val;
        setCursorHidden(val);
    }
    public void setCursorHidden(boolean val) {
        // don't set it again if it's already set
        if (getCursor() == ((val) ? hidden : standard)) return;
        // set to appropriate value
        setCursor((val) ? hidden : standard);
    }
    
    @SuppressWarnings("unused")
    public void setup() {
        entities.clear();
        
        imgWidth = 21 * renderScale;
        imgHeight = 9 * renderScale;
        
        //imgWidth = 256; imgHeight = 144;
        //imgWidth = 320; imgHeight = 240;
        //imgWidth = 480; imgHeight = 270; // 30
        //imgWidth = 640; imgHeight = 360; // 40
        //imgWidth = 800; imgHeight = 450; // 50
        
        //imgWidth = 1280; imgHeight = 720;
        //imgWidth = 1920; imgHeight = 1080;
        
        Sphere starsModel = new Sphere(200000.0f, 10, 10/*, Test3DWindow.stars*/);
        starsModel.insideOut = true;
        starsModel.fullBright = true;
        Entity stars = new Entity("Stars", starsModel);
        stars.setRotationDegrees(-90f, 0, 0);
        stars.setPosition(0, 0, 0);
        
        //Sphere planetModel = new Sphere(1.0f, 70, 70, Test3DWindow.world);
        Sphere planetModel = new Sphere(1.0f, 50, 50/*, Test3DWindow.worldBig*/);
        planetModel.fullBright = true;
        earth = new Entity("Planet", planetModel);
        earth.setRotationDegrees(-90.0f, 180f, 0.0f);
        
        
        Model axisModel = Test3DWindow.loadModel("axis.obj");
        Model mountainsModel = Test3DWindow.loadModel("mountains.obj");
        Model teapotModel = Test3DWindow.loadModel("teapot.obj");
        
        Entity axis = new Entity("Axis", axisModel);
        Entity mountains = new Entity("Mountains", mountainsModel);
        Entity teapot = new Entity("Teapot", teapotModel);
        teapot.setPosition(10.0f, 0.0f, 0.0f);
        mountains.setPosition(0.0f, -50f, 0.0f);
        
        Cube cubeModel1 = new Cube(/*Test3DWindow.world*/);
        Cube cubeModel2 = new Cube(new Color(0xbb9b9b9b, true));
        cubeModel1.fullBright = true;
        cubeModel2.fullBright = true;
        //cubeModel.setTexture(Test3DWindow.world);
        cube1 = new Entity("Cube1", cubeModel1);
        cube2 = new Entity("Cube2", cubeModel2);
        cube1.setPosition(20.0f, 0.0f, 20.0f);
        cube2.setPosition(20.0f, 0.0f, 15.0f);
        
        Line3D line11 = new Line3D();
        line11.addPoint(7.25f, 0.0f, 7.25f);
        line11.addPoint(10.5f, 0.5f, 11.5f);
        line11.addPoint(11.0f, 2.0f, 12.0f);
        line11.lineColor = Color.GREEN;
        line1 = new Entity("Line1", line11);
        line11.antiAlias = true;
        line11.lineWidth = 1;
        
        Triangle tm1 = new Triangle(new Vertex(20.0f, 0.0f, 10.0f), new Vertex(20.0f, 0.0f, 5.0f), new Vertex(20.0f, 5.0f, 5.0f));
        tm1.alwaysFaceCamera = true;
        t1 = new Entity("Triangle1", tm1);
        addEntity(t1);
        
        //Shape doom1 = load("doom_E1M1.obj");
        //addShape(doom1);
        
//        Sphere sunModel = new Sphere(1.0f, 70, 70, Test3DWindow.sun);
//        Sphere mercuryModel = new Sphere(1.0f, 70, 70, Test3DWindow.mercury);
//        Sphere venusModel = new Sphere(1.0f, 70, 70, Test3DWindow.venus);
//        Sphere marsModel = new Sphere(1.0f, 70, 70, Test3DWindow.mars);
//        Sphere jupiterModel = new Sphere(1.0f, 70, 70, Test3DWindow.jupiter);
//        Sphere saturnModel = new Sphere(1.0f, 70, 70, Test3DWindow.saturn);
//        Sphere uranusModel = new Sphere(1.0f, 70, 70, Test3DWindow.uranus);
//        Sphere neptuneModel = new Sphere(1.0f, 70, 70, Test3DWindow.neptune);
//        Sphere moonModel = new Sphere(1.0f, 70, 70, Test3DWindow.moon);
        
//        Entity sun = new Entity("sun", sunModel);
//        Entity mercury = new Entity("mercury", mercuryModel);
//        Entity venus = new Entity("venus", venusModel);
//        Entity mars = new Entity("mars", marsModel);
//        Entity jupiter = new Entity("jupiter", jupiterModel);
//        Entity saturn = new Entity("saturn", saturnModel);
//        Entity uranus = new Entity("uranus", uranusModel);
//        Entity neptune = new Entity("neptune", neptuneModel);
//        Entity moon = new Entity("moon", moonModel);
        
//        sun.setRotationDegrees(-90f, 0, 0);
//        mercury.setRotationDegrees(-90f, 0, 0);
//        venus.setRotationDegrees(-90f, 0, 0);
//        mars.setRotationDegrees(-90f, 0, 0);
//        jupiter.setRotationDegrees(-90f, 0, 0);
//        saturn.setRotationDegrees(-90f, 0, 0);
//        uranus.setRotationDegrees(-90f, 0, 0);
//        neptune.setRotationDegrees(-90f, 0, 0);
//        moon.setRotationDegrees(-90f, 0, 0);
//        
//        sunModel.fullBright = true;
//        mercuryModel.fullBright = true;
//        venusModel.fullBright = true;
//        marsModel.fullBright = true;
//        jupiterModel.fullBright = true;
//        saturnModel.fullBright = true;
//        uranusModel.fullBright = true;
//        neptuneModel.fullBright = true;
        
//        sun.setScale(10f * 109f);
//        mercury.setScale(10f * 0.383f);
//        venus.setScale(10f * 0.9499f);
        earth.setScale(10f);
//        moon.setScale(10f * 0.27f);
//        mars.setScale(10f * 0.53f);
//        jupiter.setScale(10f * 11.2f);
//        saturn.setScale(10f * 9.5f);
//        uranus.setScale(10f * 4.007f);
//        neptune.setScale(10f * 3.9f);
//        
//        sun.setPosition(10f * 11727.8144f, 0f, 0f);
//        mercury.setPosition(10f * 7188.4439f, 0, 0);
//        venus.setPosition(10f * 3245.5315f, 0, 0);
        earth.setPosition(0f, 0f, 0f);
//        moon.setPosition(-10f * 30f, 0f, 0f);
//        mars.setPosition(-10f * 6146.1273f, 0, 0);
//        jupiter.setPosition(-10f * 49302.2891f, 0, 0);
//        saturn.setPosition(-10f * 100533.0825f, 0, 0);
//        uranus.setPosition(-10f * 213029.1627f, 0, 0);
//        neptune.setPosition(-10f * 342223.2675f, 0, 0);
        
//        addEntity(stars);
//        addEntity(sun);
//        addEntity(mercury);
//        addEntity(venus);
        addEntity(earth);
//        addEntity(moon);
//        addEntity(mars);
//        addEntity(jupiter);
//        addEntity(saturn);
//        addEntity(uranus);
//        addEntity(neptune);

//        addEntity(axis);
//        addEntity(mountains);
//        addEntity(teapot);
        addEntity(cube1);
        addEntity(cube2);
        addEntity(line1);
        
        Line3D axisXLine = new Line3D(Color.RED);
        axisXLine.addPoint(-100f, 0, 0);
        axisXLine.addPoint(100f, 0, 0);
        Line3D axisYLine = new Line3D(Color.GREEN);
        axisYLine.addPoint(0, -100f, 0);
        axisYLine.addPoint(0, 100f, 0);
        Line3D axisZLine = new Line3D(Color.BLUE);
        axisZLine.addPoint(0, 0, -100f);
        axisZLine.addPoint(0, 0, 100f);
        
        Entity axisX = new Entity(axisXLine);
        Entity axisY = new Entity(axisYLine);
        Entity axisZ = new Entity(axisZLine);
        //addEntity(axisX);
        //addEntity(axisY);
        //addEntity(axisZ);
        
        renderer.onScreenResized(imgWidth, imgHeight);
        
        repaint();
    }
    
    public void addEntity(Entity entity) {
        if (entity == null) return;
        entities.add(entity);
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    //=========================
    // Internal Helper Methods
    //=========================
    
    private void createPanelImage() {
        img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    }
    
}
