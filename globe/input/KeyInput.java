package controller.globe.input;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayDeque;

public class KeyInput implements KeyListener, FocusListener {
    
    // Keep it small; VK_* you care about fit < 256. Bump if you want.
    private static final int MAX_KEYS = 256;
    
    // State arrays
    private final boolean[] down = new boolean[MAX_KEYS]; // current frame
    private final boolean[] prev = new boolean[MAX_KEYS]; // previous frame
    private final boolean[] pressed = new boolean[MAX_KEYS]; // rising edge (this tick)
    private final boolean[] released = new boolean[MAX_KEYS]; // falling edge (this tick)
    
    // Typed character queue (optional)
    private final ArrayDeque<Character> typedChars = new ArrayDeque<>();
    
    // Synchronize because AWT events fire on the EDT and your tick likely isn’t.
    private final Object lock = new Object();
    
    // Attach this to any Swing component that has focus
    public void attachTo(Component c) {
        c.addKeyListener(this);
        c.addFocusListener(this);
        c.setFocusable(true);
        c.requestFocusInWindow();
    }
    
    /** Call this exactly once per tick on your game loop thread. */
    public void tick() {
        synchronized (lock) {
            for (int i = 0; i < MAX_KEYS; i++) {
                boolean d = down[i];
                pressed[i] = d && !prev[i];
                released[i] = !d && prev[i];
                prev[i] = d;
            }
        }
    }
    
    // ======= Polling API =======
    
    /** Level-trigger: is key currently held? */
    public boolean isDown(int keyCode) {
        if (keyCode < 0 || keyCode >= MAX_KEYS) return false;
        synchronized (lock) {
            return down[keyCode];
        }
    }
    
    /** Edge-trigger: became pressed this tick? (true for 1 tick) */
    public boolean wasPressed(int keyCode) {
        if (keyCode < 0 || keyCode >= MAX_KEYS) return false;
        synchronized (lock) {
            return pressed[keyCode];
        }
    }
    
    /** Edge-trigger: became released this tick? (true for 1 tick) */
    public boolean wasReleased(int keyCode) {
        if (keyCode < 0 || keyCode >= MAX_KEYS) return false;
        synchronized (lock) {
            return released[keyCode];
        }
    }
    
    /** Pull a typed char if any (optional text input). */
    public Character pollTypedChar() {
        synchronized (lock) {
            return typedChars.pollFirst();
        }
    }
    
    /** Clear everything (useful on pause or reset). */
    public void clear() {
        synchronized (lock) {
            for (int i = 0; i < MAX_KEYS; i++) {
                down[i] = prev[i] = pressed[i] = released[i] = false;
            }
            typedChars.clear();
        }
    }
    
    // ======= KeyListener =======
    
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < MAX_KEYS) {
            synchronized (lock) {
                down[code] = true;
            }
        }
        // prevent focus traversal on arrows/Tab if you want:
        // e.consume();
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < MAX_KEYS) {
            synchronized (lock) {
                down[code] = false;
            }
        }
        // e.consume();
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        char ch = e.getKeyChar();
        synchronized (lock) {
            typedChars.addLast(ch);
        }
        // e.consume();
    }
    
    // ======= FocusListener =======
    
    @Override
    public void focusLost(FocusEvent e) {
        clear();
    }
    @Override
    public void focusGained(FocusEvent e) { /* no-op */ }
    
}
