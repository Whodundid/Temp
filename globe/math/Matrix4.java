package controller.globe.math;

import controller.globe.Entity;
import controller.globe.models.Triangle;
import eutil.strings.EStringBuilder;

public class Matrix4 {
    
    //========
    // Fields
    //========
    
    public float m00;
    public float m01;
    public float m02;
    public float m03;
    public float m10;
    public float m11;
    public float m12;
    public float m13;
    public float m20;
    public float m21;
    public float m22;
    public float m23;
    public float m30;
    public float m31;
    public float m32;
    public float m33;
    
    //==============
    // Constructors
    //==============

    public Matrix4() { identity(); }
    public Matrix4(Matrix4 m) { set(m); }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        var sb = new EStringBuilder();
        int longest = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int len = String.valueOf(get(r, c)).length();
                if (len > longest) longest = len;
            }
        }
        String f = "%" + longest + "s";
        String l = "|" + f + ", " + f + ", " + f + ", " + f + "|";
        sb.println(String.format(l, m00, m01, m02, m03));
        sb.println(String.format(l, m10, m11, m12, m13));
        sb.println(String.format(l, m20, m21, m22, m23));
        sb.println(String.format(l, m30, m31, m32, m33));
        return sb.toString();
    }
    
    //=========
    // Methods
    //=========

    public Matrix4 identity() {
        m00 = 1.0f; m10 = 0.0f; m20 = 0.0f; m30 = 0.0f;
        m01 = 0.0f; m11 = 1.0f; m21 = 0.0f; m31 = 0.0f;
        m02 = 0.0f; m12 = 0.0f; m22 = 1.0f; m32 = 0.0f;
        m03 = 0.0f; m13 = 0.0f; m23 = 0.0f; m33 = 1.0f;
        return this;
    }
    
    public static Matrix4 makeIndentity() {
        return new Matrix4();
    }
    
    public static Matrix4 makeTransform(Entity e) {
        return makeTransform(e.position, e.rotation, e.scale);
    }
    
    public static Matrix4 makeTransform(Vector3 pos, Vector3 rot, Vector3 scale) {
        Matrix4 m = new Matrix4();
        m.scale(scale);
        m.rotateXYZ(rot);
        m.translate(pos);
        return m;
    }
    
    public Matrix4 scale(Vector3 scale) { return scale(scale.x, scale.y, scale.z); }
    public Matrix4 scale(float x, float y, float z) { return set(multiply(makeScale(x, y, z))); }
    public Matrix4 scale(float value) { return set(multiply(makeScale(value))); }
    
    public Matrix4 translate(Vector3 pos) { return translate(pos.x, pos.y, pos.z); }
    public Matrix4 translate(float x, float y, float z) { return set(multiply(makeTranslation(x, y, z))); }
    
    public Matrix4 rotateXYZ(Vector3 rot) { return rotateXYZ(rot.x, rot.y, rot.z); }
    public Matrix4 rotateXYZ(float x, float y, float z) { return rotateX(x).rotateY(y).rotateZ(z); }
    
    public Matrix4 rotateXYZ_Degrees(Vector3 rot) {
        return rotateXYZ_Degrees(rot.x, rot.y, rot.z);
    }
    public Matrix4 rotateXYZ_Degrees(float x, float y, float z) {
        x = (float) Math.toRadians(x);
        y = (float) Math.toRadians(y);
        z = (float) Math.toRadians(z);
        return rotateXYZ(x, y, z);
    }
    
    public Matrix4 rotateX(float angleRad) { return set(multiply(makeRotationX(angleRad))); }
    public Matrix4 rotateY(float angleRad) { return set(multiply(makeRotationY(angleRad))); }
    public Matrix4 rotateZ(float angleRad) { return set(multiply(makeRotationZ(angleRad))); }
    
    public static Matrix4 makeScale(float value) { return makeScale(value, value, value); }
    public static Matrix4 makeScale(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m00 *= x;
        m.m11 *= y;
        m.m22 *= z;
        return m;
    }
    
    public static Matrix4 makeTranslation(Vector3 v) { return makeTranslation(v.x, v.y, v.z); }
    public static Matrix4 makeTranslation(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m03 = x;
        m.m13 = y;
        m.m23 = z;
        return m;
    }
    
    public static Matrix4 makeRotationX(float angleRad) {
        Matrix4 m = new Matrix4();
        m.m11 = (float) Math.cos(angleRad);
        m.m12 = (float) -Math.sin(angleRad);
        m.m21 = (float) Math.sin(angleRad);
        m.m22 = (float) Math.cos(angleRad);
        return m;
    }
    
    public static Matrix4 makeRotationY(float angleRad) {
        Matrix4 m = new Matrix4();
        m.m00 = (float) Math.cos(angleRad);
        m.m02 = (float) Math.sin(angleRad);
        m.m20 = (float) -Math.sin(angleRad);
        m.m22 = (float) Math.cos(angleRad);
        return m;
    }
    
    public static Matrix4 makeRotationZ(float angleRad) {
        Matrix4 m = new Matrix4();
        m.m00 = (float) Math.cos(angleRad);
        m.m01 = (float) -Math.sin(angleRad);
        m.m10 = (float) Math.sin(angleRad);
        m.m11 = (float) Math.cos(angleRad);
        return m;
    }
    
    public static Matrix4 rotate(float angleDeg, Vector3 rot) { return rotate(angleDeg, rot.x, rot.y, rot.z); }
    public static Matrix4 rotate(float angleDeg, float x, float y, float z) {
        Matrix4 m = new Matrix4();
        float angleRad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float C = 1.0f - cos;

        m.m00 = x * x * C + cos;
        m.m01 = x * y * C - z * sin;
        m.m02 = x * z * C + y * sin;

        m.m10 = y * x * C + z * sin;
        m.m11 = y * y * C + cos;
        m.m12 = y * z * C - x * sin;

        m.m20 = z * x * C - y * sin;
        m.m21 = z * y * C + x * sin;
        m.m22 = z * z * C + cos;

        return m;
    }
    
//    public static Matrix4 projection(float fovDegrees, float aspectRatio, float near, float far) {
//        float fovRad = 1.0f / (float) Math.tan(fovDegrees * 0.5f / 180.0f * (float) Math.PI);
//        Matrix4 m = new Matrix4();
//        m.m00 = aspectRatio * fovRad;
//        m.m11 = fovRad;
//        m.m22 = far / (far - near);
//        m.m32 = (-far * near) / (far - near);
//        m.m23 = 1.0f;
//        m.m33 = 0.0f;
//        return m;
//    }
    
//    public static Matrix4 projection(float fovDeg, float aspectWidthOverHeight, float near, float far) {
//        float f = 1f / (float)Math.tan(Math.toRadians(fovDeg) * 0.5);
//        Matrix4 m = new Matrix4();
//        m.m00 = f / aspectWidthOverHeight;  // <- uses W/H
//        m.m11 = f;
//        m.m22 =  far / (far - near);
//        m.m23 =  1f;
//        m.m32 = (-far * near) / (far - near);
//        m.m33 =  0f;
//        return m;
//    }

    public static Matrix4 projection(float fovDeg, float aspectWidthOverHeight, float near, float far) {
        float f = 1f / (float)Math.tan(Math.toRadians(fovDeg) * 0.5);
        Matrix4 m = new Matrix4();
        float r = aspectWidthOverHeight;
        m.m00 = f / r;
        m.m11 = f;
        m.m22 = far / (far-near);
        m.m23 = -far*near / (far-near);
        m.m32 = 1f;
        m.m33 = 0f;
        return m;
    }
    
    public Matrix4 multiply(Matrix4 other) {
        Matrix4 m = new Matrix4();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                m.set(r, c, get(r, 0) * other.get(0, c)
                          + get(r, 1) * other.get(1, c)
                          + get(r, 2) * other.get(2, c)
                          + get(r, 3) * other.get(3, c)
                );
            }
        }
        return m;
    }
    
    public Vector3 multiply(Vector3 v) {
        Vector3 r = new Vector3();
        r.x = v.x * m00 + v.y * m01 + v.z * m02 + v.w * m03;
        r.y = v.x * m10 + v.y * m11 + v.z * m12 + v.w * m13;
        r.z = v.x * m20 + v.y * m21 + v.z * m22 + v.w * m23;
        r.w = v.x * m30 + v.y * m31 + v.z * m32 + v.w * m33;
        return r;
    }
    
    public Triangle multiply(Triangle tri) {
        Triangle t = new Triangle(tri);
        t.v0.pos = multiply(t.v0.pos);
        t.v1.pos = multiply(t.v1.pos);
        t.v2.pos = multiply(t.v2.pos);
        return t;
    }
    
//    public static Matrix4 pointAt(Vector3 pos, Vector3 target, Vector3 up) {
//        // calculate new forward direction
//        Vector3 newForward = target.sub(pos).norm();
//        
//        // calculate new up direction
//        Vector3 a = newForward.mul(up.dot(newForward));
//        Vector3 newUp = up.sub(a).norm();
//        
//        // cross to get right
//        Vector3 newRight = newUp.cross(newForward);
//        
//        // construct dimension and translation matrix
//        Matrix4 m = new Matrix4();
//        m.m00 = newRight.x;   m.m01 = newRight.y;   m.m02 = newRight.z;   m.m03 = 0.0f;
//        m.m10 = newUp.x;      m.m11 = newUp.y;      m.m12 = newUp.z;      m.m13 = 0.0f;
//        m.m20 = newForward.x; m.m21 = newForward.y; m.m22 = newForward.z; m.m23 = 0.0f;
//        m.m30 = pos.x;        m.m31 = pos.y;        m.m32 = pos.z;        m.m33 = 1.0f;
//        return m;
//    }
    
//    public static Matrix4 pointAt(Vector3 eye, Vector3 target, Vector3 upHint) {
//        Vector3 f = target.sub(eye).norm();          // forward (camera -Z later)
//        Vector3 r = upHint.cross(f).norm();          // right  (note order!)
//        Vector3 u = f.cross(r);                      // recompute up
//
//        Matrix4 m = new Matrix4();
//        // world->camera orientation (camera axes as columns)
//        m.m00 = r.x; m.m01 = r.y; m.m02 = r.z; m.m03 = 0f;
//        m.m10 = u.x; m.m11 = u.y; m.m12 = u.z; m.m13 = 0f;
//        m.m20 =-f.x; m.m21 =-f.y; m.m22 =-f.z; m.m23 = 0f; // look down -Z
//        m.m30 = eye.x; m.m31 = eye.y; m.m32 = eye.z; m.m33 = 1f;
//        return m;
//    }
    
    public static Matrix4 lookAt(Vector3 eye, Vector3 target, Vector3 up) {
        Vector3 f = target.sub(eye).normalize();      // forward
        Vector3 s = f.cross(up).normalize();          // right
        Vector3 u = s.cross(f);                  // up (re-orthogonalized)

        Matrix4 m = new Matrix4();
        // rotation (rows are camera axes in world, for column vectors put them across rows)
        m.m00 =  s.x; m.m01 =  s.y; m.m02 =  s.z; m.m03 = -s.dot(eye);
        m.m10 =  u.x; m.m11 =  u.y; m.m12 =  u.z; m.m13 = -u.dot(eye);
        m.m20 = -f.x; m.m21 = -f.y; m.m22 = -f.z; m.m23 =  f.dot(eye);
        m.m30 =    0; m.m31 =    0; m.m32 =    0; m.m33 =  1;
        return m;
    }
    
    public Matrix4 inverse() {
        Matrix4 m = new Matrix4();
        m.m00 = m00; m.m01 = m10; m.m02 = m20; m.m03 = 0.0f;
        m.m10 = m01; m.m11 = m11; m.m12 = m21; m.m13 = 0.0f;
        m.m20 = m02; m.m21 = m12; m.m22 = m22; m.m23 = 0.0f;
        m.m30 = -(m30 * m.m00 + m31 * m.m10 + m32 * m.m20);
        m.m31 = -(m30 * m.m01 + m31 * m.m11 + m32 * m.m21);
        m.m32 = -(m30 * m.m02 + m31 * m.m12 + m32 * m.m22);
        m.m33 = 1.0f;
        return m;
    }
    
    /** Column-vector TRS: M = T * Rz * Ry * Rx * S (edit to taste). */
    public static Matrix4 composeTRS(Vector3 pos, Vector3 rotRad, Vector3 scale) {
        Matrix4 T = makeTranslation(pos.x, pos.y, pos.z);
        Matrix4 Rx = makeRotationX(rotRad.x);
        Matrix4 Ry = makeRotationY(rotRad.y);
        Matrix4 Rz = makeRotationZ(rotRad.z);
        Matrix4 S  = makeScale(scale.x, scale.y, scale.z);
        return T.multiply(Rz).multiply(Ry).multiply(Rx).multiply(S);
    }
    
    public float get(int r, int c) {
        switch (r * 4 + c) {
        case 0:  return m00;
        case 1:  return m01;
        case 2:  return m02;
        case 3:  return m03;
        case 4:  return m10;
        case 5:  return m11;
        case 6:  return m12;
        case 7:  return m13;
        case 8:  return m20;
        case 9:  return m21;
        case 10: return m22;
        case 11: return m23;
        case 12: return m30;
        case 13: return m31;
        case 14: return m32;
        case 15: return m33;
        }
        throw new IllegalArgumentException("The row/column position of: '" + r + ", " + c + "' is out of bounds for this Matrix4!");
    }
    
    public Matrix4 set(int r, int c, float v) {
        switch (r * 4 + c) {
        case 0:  m00 = v; break;
        case 1:  m01 = v; break;
        case 2:  m02 = v; break;
        case 3:  m03 = v; break;
        case 4:  m10 = v; break;
        case 5:  m11 = v; break;
        case 6:  m12 = v; break;
        case 7:  m13 = v; break;
        case 8:  m20 = v; break;
        case 9:  m21 = v; break;
        case 10: m22 = v; break;
        case 11: m23 = v; break;
        case 12: m30 = v; break;
        case 13: m31 = v; break;
        case 14: m32 = v; break;
        case 15: m33 = v; break;
        default:
        throw new IllegalArgumentException("The row/column position of: '" + r + ", " + c + "' is out of bounds for this Matrix4!");
        }
        return this;
    }
    
    public Matrix4 set(Matrix4 m) {
        m00 = m.m00;
        m01 = m.m01;
        m02 = m.m02;
        m03 = m.m03;
        m10 = m.m10;
        m11 = m.m11;
        m12 = m.m12;
        m13 = m.m13;
        m20 = m.m20;
        m21 = m.m21;
        m22 = m.m22;
        m23 = m.m23;
        m30 = m.m30;
        m31 = m.m31;
        m32 = m.m32;
        m33 = m.m33;
        return this;
    }
    
}