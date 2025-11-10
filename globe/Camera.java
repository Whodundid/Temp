package controller.globe;

import java.awt.event.KeyEvent;

import controller.globe.math.Matrix4;
import controller.globe.math.Quaternion;
import controller.globe.math.Vector3;
import eutil.math.ENumUtil;

public class Camera {
    
    //========
    // Fields
    //========
    
    public final Vector3 position = new Vector3(0, 0, 0);
    /** rotation.x = pitch (deg), rotation.y = yaw (deg), rotation.z = roll (deg) */
//    public final Vector3 rotation = new Vector3(0, 0, 0);
    
    private Quaternion orientation = new Quaternion(); // identity
    public boolean invertMouseY = false;
    
    // Tunables
    public float moveSpeed = 0.65f; // base units per key press (or per tick if you call it every tick)
    public float rotSpeed = 2.0f; // degrees per key press
    public float maxPitchDeg = 89.5f; // clamp to avoid gimbal lock
    
    /** Mouse sensitivities (degrees per pixel). */
    public float mouseYawPerPixel = 0.12f;
    public float mousePitchPerPixel = 0.12f;
    
    // Authoritative angles (degrees)
    private float yawDegAccum = 0f;
    private float pitchDegAccum = 0f;
    private float rollDegAccum = 0f; // preserves intentional roll keys
    
    //=========
    // Getters
    //=========
    
    /**
     * Build a proper OpenGL-style view matrix (camera looks down -Z in its
     * local space).
     */
    public Matrix4 getViewMatrix() {
        Quaternion.Basis b = orientation.toBasis();
        Vector3 eye = position;
        Vector3 center = eye.add(b.forward); // look along camera forward
        return Matrix4.lookAt(eye, center, b.up);
    }
    
    //=========
    // Methods
    //=========
    
    /** Move in camera-local space: x=right, y=up, z=forward. */
    public void moveLocal(float dx, float dy, float dz) {
        Quaternion.Basis b = orientation.toBasis();
        position.addT(b.right.mul(dx)).addT(b.up.mul(dy)).addT(b.forward.mul(dz));
    }
    
    public void resetPosition() {
        position.set(0, 0, 0);
    }
    
    public void resetOrientation() {
//        orientation = Quaternion.identity();
        yawDegAccum = 0f;
        pitchDegAccum = 0f;
        rollDegAccum = 0f;
        orientation = Quaternion.identity();
    }

    /** Snap roll to 0 while keeping your current forward direction. */
    public void zeroRollKeepForward() {
        Quaternion.Basis b = orientation.toBasis();
        Vector3 f = b.forward;                    // keep look direction
        Vector3 r = new Vector3(0,1,0).cross(f);  // recompute right from world-up × forward
        if (r.len() * r.len() < 1e-6f) {          // looking straight up/down: choose a fallback
            r = new Vector3(1,0,0);
        }
        r = r.normalize();
        Vector3 u = f.cross(r).normalize();
        orientation = Quaternion.fromBasis(r, u, f);
//        rollDegAccum = 0f;
//        setOrientationYawPitchRollDeg(yawDegAccum, pitchDegAccum, rollDegAccum);
    }

    /** Set absolute orientation from yaw/pitch/roll (degrees), intrinsic about LOCAL axes. */
    public void setOrientationYawPitchRollDeg(float yawDeg, float pitchDeg, float rollDeg) {
        Quaternion q = Quaternion.identity();
        Quaternion qYaw   = Quaternion.fromAxisAngle(new Vector3(0,1,0), (float) Math.toRadians(yawDeg));
        q = qYaw.mul(q);
        Vector3 rightAfterYaw = q.toBasis().right;
        Quaternion qPitch = Quaternion.fromAxisAngle(rightAfterYaw, (float) Math.toRadians(pitchDeg));
        q = qPitch.mul(q);
        Vector3 forwardAfterYawPitch = q.toBasis().forward;
        Quaternion qRoll  = Quaternion.fromAxisAngle(forwardAfterYawPitch, (float) Math.toRadians(rollDeg));
        q = qRoll.mul(q).normalize();
        orientation = q;
    }

    /** Set absolute orientation to look along a direction, with an up hint; optional extra rollDeg. */
    public void setOrientationLook(Vector3 forward, Vector3 upHint, float rollDeg) {
        Vector3 f = new Vector3(forward).normalize();
        Vector3 r = new Vector3(upHint).cross(f).normalize();
        if (r.len() * r.len() < 1e-6f) r.set(1,0,0); // guard singularity
        Vector3 u = f.cross(r).normalize();
        Quaternion q = Quaternion.fromBasis(r, u, f);
        if (rollDeg != 0f) {
            Quaternion qRoll = Quaternion.fromAxisAngle(f, (float)Math.toRadians(rollDeg));
            q = qRoll.mul(q).normalize();
        }
        orientation = q;
    }

    /** Smoothly blend to a target orientation (0..1). Useful for reset animations. */
    public void slerpTo(Quaternion target, float t) {
        orientation = Quaternion.slerp(orientation, target, t);
    }

    /** Get the current orientation (if you want to store/restore). */
    public Quaternion getOrientation() {
        return orientation;
    }

    /** Set the orientation directly (if you’ve saved one). */
    public void setOrientation(Quaternion q) {
        orientation = new Quaternion(q.x,q.y,q.z,q.w).normalize();
    }

    
    // ========= Key / mouse helpers (keep or swap for your input system) =========
    
    public void onKeyPressed(int key) {
        moveSpeed = 0.75f;
        
        // Optional distance-based speed curve (kept from your version, simplified)
        Vector3 p = position;
        float distToCenter = (float) Math.sqrt((p.x * p.x) + (p.y * p.y) + (p.z * p.z));
        float surface = 10.1f;
        float distToSurface = distToCenter - surface;
        float slowdownStart = 15f;
        float slowdownRange = (slowdownStart - surface);
        
        float minMod = 0.005f;
        float maxMod = 1.0f;
        
        float speedModifier;
        if (distToSurface >= (slowdownStart - surface)) speedModifier = maxMod;
        else if (distToSurface < 0) speedModifier = minMod;
        else {
            speedModifier = (distToSurface) / slowdownRange;
            speedModifier = ENumUtil.clamp(speedModifier, minMod, maxMod);
        }
        
        float move = moveSpeed * speedModifier;
        float rot = rotSpeed;
        
        // Camera local WASD
        if (key == KeyEvent.VK_W) moveLocal(0, 0, -move);
        if (key == KeyEvent.VK_S) moveLocal(0, 0,  move);
        if (key == KeyEvent.VK_A) moveLocal(-move, 0, 0);
        if (key == KeyEvent.VK_D) moveLocal( move, 0, 0);

        // World-absolute up/down
        if (key == KeyEvent.VK_SPACE) moveWorld(0,  move, 0);
        if (key == KeyEvent.VK_SHIFT) moveWorld(0, -move, 0);
        
        if (key == KeyEvent.VK_Q) addRollDegrees(rot);
        if (key == KeyEvent.VK_E) addRollDegrees(-rot);
        
//        switch (e.getKeyCode()) {
//        case KeyEvent.VK_W -> moveLocal(0, 0, -move);   // forward (toward -Z in view space)
//        case KeyEvent.VK_S -> moveLocal(0, 0, move);   // backward
//        case KeyEvent.VK_A -> moveLocal(-move, 0, 0);   // left
//        case KeyEvent.VK_D -> moveLocal(move, 0, 0);   // right
//        
//        // world-absolute vertical, ignores camera roll/pitch
//        case KeyEvent.VK_SPACE -> moveWorld(0, move, 0); // up (world)
//        case KeyEvent.VK_SHIFT -> moveWorld(0, -move, 0); // down (world)
//        
//        //case KeyEvent.VK_UP -> addRotationDeg(-rot, 0, 0); // pitch up
//        //case KeyEvent.VK_DOWN -> addRotationDeg(rot, 0, 0); // pitch down
//        //case KeyEvent.VK_LEFT -> addRotationDeg(0, rot, 0); // yaw left
//        //case KeyEvent.VK_RIGHT -> addRotationDeg(0, -rot, 0); // yaw right
//        case KeyEvent.VK_Q -> addRollDegrees(-rot); // roll CCW
//        case KeyEvent.VK_E -> addRollDegrees(rot); // roll CW
//        }
    }
    
    /** Feed raw mouse delta (pixels). Positive dx = mouse moved right, dy = mouse moved down. */
    public void onMouseDelta(int dx, int dy) {
//        float yawDeg   = -dx * mouseYawPerPixel;
//        float pitchDeg = (invertMouseY ? +dy : -dy) * mousePitchPerPixel;
//
//        Quaternion.Basis b = orientation.toBasis();
//        Quaternion qYaw   = Quaternion.fromAxisAngle(b.up,     (float)Math.toRadians(yawDeg));
//        Quaternion qPitch = Quaternion.fromAxisAngle(b.right,  (float)Math.toRadians(pitchDeg));
//
//        // intrinsic yaw then pitch about CURRENT local axes
//        orientation = qPitch.mul(qYaw).mul(orientation).normalize();
        float dYaw   = -dx * mouseYawPerPixel;
        float dPitch = (invertMouseY ? +dy : -dy) * mousePitchPerPixel;

        yawDegAccum   += dYaw;
        pitchDegAccum += dPitch;

        // prevent flipping
        pitchDegAccum = ENumUtil.clamp(pitchDegAccum, -maxPitchDeg, +maxPitchDeg);

        // Rebuild orientation fresh from angles (intrinsic yaw->pitch->roll)
        setOrientationYawPitchRollDeg(yawDegAccum, pitchDegAccum, rollDegAccum);
    }
    
    // --- Keyboard roll (Q/E) rotates around LOCAL forward axis ---
    public void addRollDegrees(float dRollDeg) {
//        Quaternion.Basis b = orientation.toBasis();
//        Quaternion qRoll = Quaternion.fromAxisAngle(b.forward, (float)Math.toRadians(dRollDeg));
//        orientation = qRoll.mul(orientation).normalize();
        rollDegAccum += dRollDeg;
        setOrientationYawPitchRollDeg(yawDegAccum, pitchDegAccum, rollDegAccum);
    }
    
    public void moveWorld(float dx, float dy, float dz) {
        position.x += dx;
        position.y += dy;
        position.z += dz;
    }
    
    // Optional helpers to set or read Euler if you need UI sliders:
    public void setEulerDegrees(Vector3 eulerDeg) {
        // build from yaw-pitch-roll about local axes
        Quaternion qy = Quaternion.fromAxisAngle(new Vector3(0,1,0), (float)Math.toRadians(eulerDeg.y));
        Quaternion qx = Quaternion.fromAxisAngle(new Vector3(1,0,0), (float)Math.toRadians(eulerDeg.x));
        Quaternion qz = Quaternion.fromAxisAngle(new Vector3(0,0,-1), (float)Math.toRadians(eulerDeg.z)); // roll right-handed
        orientation = qz.mul(qx).mul(qy).normalize();
    }
    
}
