package controller.globe;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import eutil.math.ENumUtil;

public class Camera {
    
    //========
    // Fields
    //========
    
    public final Vector3 position;
    public final Vector3 rotation;
    
    //==============
    // Constructors
    //==============
    
    public Camera() {
        this(new Vector3(), new Vector3());
    }
    
    public Camera(float x, float y, float z) {
        this(new Vector3(x, y, z), new Vector3());
    }
    
    public Camera(float x, float y, float z, float rotX, float rotY, float rotZ) {
        this(new Vector3(x, y, z), new Vector3(rotX, rotY, rotZ));
    }
    
    public Camera(Vector3 positionIn, Vector3 rotationIn) {
        position = positionIn;
        rotation = rotationIn;
    }
    
    //=========
    // Methods
    //=========
    
    public void move(float x, float y, float z) {
        position.addT(x, y, z);
    }
    
    public void updateLook(float rotX, float rotY, float rotZ) {
        rotation.addT(rotX, rotY, rotZ);
        rotation.x = ENumUtil.clamp(rotation.x, -90.0f, 90.0f);
        rotation.y %= 360.0;
        if (rotation.y < 0) rotation.y += 360.0f;
    }
    
    public void reset() {
        resetPosition();
        resetLook();
    }
    
    public void resetPosition() {
        position.set(0, 0, 30);
    }
    
    public void resetLook() {
        rotation.set(0, 0, 0);
    }
    
    public void onKeyPressed(KeyEvent e) {
        Vector3 p = new Vector3(position);
        float distToCenter = (float) Math.sqrt((p.x * p.x) + (p.y * p.y) + (p.z * p.z));
        float speedModifier = (float) ((Math.pow(distToCenter, Math.E) / 100.0f) - 5.3f);
        speedModifier = ENumUtil.clamp(speedModifier, 0.1f, 200f);
        float amount = 0.01f * speedModifier;
        float rotAmount = 2f;
        
        float y$1 = (float) Math.sin(Math.toRadians(rotation.x));
        float y$2 = (float) Math.cos(Math.toRadians(rotation.x));
        
        float y = y$1 * amount;
        float x = (float) Math.sin(Math.toRadians(rotation.y)) * amount * y$2;
        float z = (float) Math.cos(Math.toRadians(rotation.y)) * amount * y$2;
        
        if (e.getKeyCode() == KeyEvent.VK_W) move(x, y, -z);
        if (e.getKeyCode() == KeyEvent.VK_S) move(-x, -y, z);
        if (e.getKeyCode() == KeyEvent.VK_A) move(-z, 0, -x);
        if (e.getKeyCode() == KeyEvent.VK_D) move(z, 0, x);
        if (e.getKeyCode() == KeyEvent.VK_SPACE) move(0, amount, 0);
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) move(0, -amount, 0);
        if (e.getKeyCode() == KeyEvent.VK_UP) updateLook(rotAmount, 0, 0);
        if (e.getKeyCode() == KeyEvent.VK_DOWN) updateLook(-rotAmount, 0, 0);
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) updateLook(0, rotAmount, 0);
        if (e.getKeyCode() == KeyEvent.VK_LEFT) updateLook(0, -rotAmount, 0);
    }
    
    public void onMouseMoved(MouseEvent e) {
        
    }
    
}
