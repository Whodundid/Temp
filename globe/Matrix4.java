package controller.globe;

import eutil.strings.EStringBuilder;

public class Matrix4 {
    
    //========
    // Fields
    //========
    
    float m00, m01, m02, m03;
    float m10, m11, m12, m13;
    float m20, m21, m22, m23;
    float m30, m31, m32, m33;
    
    //==============
    // Constructors
    //==============
    
    public Matrix4() {
        setIdentity();
    }
    
    public Matrix4(Matrix4 in) {
        set(in);
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        var sb = new EStringBuilder();
        int longest = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int l = String.valueOf(get(r, c)).length();
                if (l > longest) longest = l;
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
    
    public Matrix4 setIdentity() {
        m00 = 1.0f; m01 = 0.0f; m02 = 0.0f; m03 = 0.0f;
        m10 = 0.0f; m11 = 1.0f; m12 = 0.0f; m13 = 0.0f;
        m20 = 0.0f; m21 = 0.0f; m22 = 1.0f; m23 = 0.0f;
        m30 = 0.0f; m31 = 0.0f; m32 = 0.0f; m33 = 1.0f;
        return this;
    }
    
    public static Matrix4 makeIdentity() {
        return new Matrix4();
    }
    
    public static Matrix4 makeTransform(Shape shape) {
        return makeTransform(shape.position, shape.rotation, shape.scale);
    }
    public static Matrix4 makeTransform(Vector pos, Vector rot, Vector scale) {
        Matrix4 m = new Matrix4();
        m.scale(scale);
        m.rotateXYZ(rot);
        m.translate(pos);
        return m;
    }
    
    public Matrix4 scale(Vector scale) { return scale(scale.x, scale.y, scale.z); }
    public Matrix4 scale(float x, float y, float z) { return set(multiply(makeScale(x, y, z))); }
    public Matrix4 scale(float value) { return set(multiply(makeScale(value))); }
    
    public Matrix4 translate(Vector pos) { return translate(pos.x, pos.y, pos.z); }
    public Matrix4 translate(float x, float y, float z) { return set(multiply(makeTranslation(x, y, z))); }
    
    public Matrix4 rotateXYZ(Vector rot) { return rotateXYZ(rot.x, rot.y, rot.z); }
    public Matrix4 rotateXYZ(float x, float y, float z) { return rotateX(x).rotateY(y).rotateZ(z); }
    public Matrix4 rotateX(float angleRad) { return set(multiply(makeRotationX(angleRad))); }
    public Matrix4 rotateY(float angleRad) { return set(multiply(makeRotationY(angleRad))); }
    public Matrix4 rotateZ(float angleRad) { return set(multiply(makeRotationZ(angleRad))); }
    
    public static Matrix4 makeRotationX(float angleRad) {
        Matrix4 m = new Matrix4();
        m.m11 = (float) Math.cos(angleRad);
        m.m12 = (float) Math.sin(angleRad);
        m.m21 = (float) -Math.sin(angleRad);
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
        m.m01 = (float) Math.sin(angleRad);
        m.m10 = (float) -Math.sin(angleRad);
        m.m11 = (float) Math.cos(angleRad);
        return m;
    }
    
    public static Matrix4 makeTranslation(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m30 = x;
        m.m31 = y;
        m.m32 = z;
        return m;
    }
    
    public static Matrix4 makeScale(float value) { return makeScale(value, value, value); }
    public static Matrix4 makeScale(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m00 *= x;
        m.m11 *= y;
        m.m22 *= z;
        return m;
    }
    
    public static Matrix4 makeProjection(float fov, float aspectRatio, float near, float far) {
        float fovRad = 1.0f / (float) Math.tan(fov * 0.5f / 180.0f * 3.14159f);
        Matrix4 projection = new Matrix4();
        projection.m00 = aspectRatio * fovRad;
        projection.m11 = fovRad;
        projection.m22 = far / (far - near);
        projection.m32 = (-far * near) / (far - near);
        projection.m23 = 1.0f;
        projection.m33 = 0.0f;
        return projection;
    }
    
    public Matrix4 multiply(Matrix4 o) { return multiply(this, o); }
    public Matrix4 multiply(Matrix4 t, Matrix4 o) {
        Matrix4 m = new Matrix4();
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                m.set(r, c, (t.get(r, 0) * o.get(0, c)) +
                            (t.get(r, 1) * o.get(1, c)) +
                            (t.get(r, 2) * o.get(2, c)) +
                            (t.get(r, 3) * o.get(3, c)));
            }
        }
        return m;
    }
    
    public Matrix4 pointAt(Vector pos, Vector target, Vector up) {
        // calculate new forward direction
        Vector newForward = target.sub(pos).norm();
        
        // calculate new up direction
        Vector a = newForward.mul(up.dot(newForward));
        Vector newUp = up.sub(a).norm();
        
        // new right direction is cross product
        Vector newRight = newUp.cross(newForward);
        
        // construct dimensioning and translation matrix
        Matrix4 m = new Matrix4();
        m.m00 = newRight.x;   m.m01 = newRight.y;   m.m02 = newRight.z;   m.m03 = 0.0f;
        m.m10 = newUp.x;      m.m11 = newUp.y;      m.m12 = newUp.z;      m.m13 = 0.0f;
        m.m20 = newForward.x; m.m21 = newForward.y; m.m22 = newForward.z; m.m23 = 0.0f;
        m.m30 = pos.x;        m.m31 = pos.y;        m.m32 = pos.z;        m.m33 = 1.0f;
        return m;
    }
    
    public static Matrix4 quickInverse(Matrix4 in) {
        Matrix4 m = new Matrix4();
        m.m00 = in.m00; m.m01 = in.m10; m.m02 = in.m20; m.m03 = 0.0f;
        m.m10 = in.m01; m.m11 = in.m11; m.m12 = in.m21; m.m13 = 0.0f;
        m.m20 = in.m02; m.m21 = in.m12; m.m22 = in.m22; m.m23 = 0.0f;
        m.m30 = -(in.m30 * in.m00 + in.m31 * in.m10 + in.m32 * in.m20);
        m.m31 = -(in.m30 * in.m01 + in.m31 * in.m11 + in.m32 * in.m21);
        m.m32 = -(in.m30 * in.m02 + in.m31 * in.m12 + in.m32 * in.m22);
        m.m33 = 1.0f;
        return m;
    }
    
    public static Matrix4 transpose(Matrix4 in) {
        Matrix4 m = new Matrix4();
        m.m00 = in.m00; m.m01 = in.m10; m.m02 = in.m20; m.m03 = in.m30;
        m.m10 = in.m01; m.m11 = in.m11; m.m12 = in.m21; m.m13 = in.m31;
        m.m20 = in.m02; m.m21 = in.m12; m.m22 = in.m22; m.m23 = in.m32;
        m.m30 = in.m03; m.m31 = in.m13; m.m32 = in.m23; m.m33 = in.m33;
        return m;
    }
    
    public Triangle multiply(Triangle t) {
        Triangle r = new Triangle(t);
        r.v0 = multiply(r.v0);
        r.v1 = multiply(r.v1);
        r.v2 = multiply(r.v2);
        return r;
    }
    
    public Vector multiply(Vector i) { return mulitply(i, this); }
    public static Vector mulitply(Vector i, Matrix4 m) {
        Vector o = new Vector();
        o.x = i.x * m.m00 + i.y * m.m10 + i.z * m.m20 + m.m30;
        o.y = i.x * m.m01 + i.y * m.m11 + i.z * m.m21 + m.m31;
        o.z = i.x * m.m02 + i.y * m.m12 + i.z * m.m22 + m.m32;
        o.w = i.x * m.m03 + i.y * m.m13 + i.z * m.m23 + m.m33;
        return o;
    }
    
    //=========
    // Getters
    //=========
    
    public float get(int r, int c) {
        int i = r * 4 + c;
        return switch (i) {
        case  0 -> m00;
        case  1 -> m01;
        case  2 -> m02;
        case  3 -> m03;
        case  4 -> m10;
        case  5 -> m11;
        case  6 -> m12;
        case  7 -> m13;
        case  8 -> m20;
        case  9 -> m21;
        case 10 -> m22;
        case 11 -> m23;
        case 12 -> m30;
        case 13 -> m31;
        case 14 -> m32;
        case 15 -> m33;
        default -> Float.NaN;
        };
    }
    
    //=========
    // Setters
    //=========
    
    public Matrix4 set(Matrix4 m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m03 = m.m03;
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m13 = m.m13;
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        this.m23 = m.m23;
        this.m30 = m.m30;
        this.m31 = m.m31;
        this.m32 = m.m32;
        this.m33 = m.m33;
        return this;
    }
    
    public Matrix4 set(int r, int c, float v) {
        int i = r * 4 + c;
        switch (i) {
        case  0: m00 = v; break;
        case  1: m01 = v; break;
        case  2: m02 = v; break;
        case  3: m03 = v; break;
        case  4: m10 = v; break;
        case  5: m11 = v; break;
        case  6: m12 = v; break;
        case  7: m13 = v; break;
        case  8: m20 = v; break;
        case  9: m21 = v; break;
        case 10: m22 = v; break;
        case 11: m23 = v; break;
        case 12: m30 = v; break;
        case 13: m31 = v; break;
        case 14: m32 = v; break;
        case 15: m33 = v; break;
        };
        return this;
    }
    
    public Matrix4 set(float m00, float m01, float m02, float m03,
                       float m10, float m11, float m12, float m13,
                       float m20, float m21, float m22, float m23,
                       float m30, float m31, float m32, float m33)
    {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        return this;
    }
    
}