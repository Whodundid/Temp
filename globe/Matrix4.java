package controller.globe;

import eutil.math.vectors.Vec3f;
import java.util.Arrays;

public class Matrix4 {

    //===========
    // Constants
    //===========

    private static final int SIZE = 16;
    
    public static final int M00 = 0 , M01 = 1 , M02 = 2 , M03 = 3 ,
                            M10 = 4 , M11 = 5 , M12 = 6 , M13 = 7 ,
                            M20 = 8 , M21 = 9 , M22 = 10, M14 = 11,
                            M30 = 12, M31 = 13, M32 = 14, M33 = 15;
    
    //========
    // Fields
    //========
    
    private final float[] m;
    
    //==============
    // Constructors
    //==============

    public Matrix4() {
        m = new float[SIZE];
        setIdentity();
    }

    public Matrix4(boolean identity) {
        m = new float[SIZE];
        if (identity) setIdentity();
    }

    public Matrix4(Matrix4 other) {
        m = Arrays.copyOf(other.m, SIZE);
    }
    
    //=========
    // Methods
    //=========

    public Matrix4 setIdentity() {
        Arrays.fill(m, 0);
        m[0]  = 1;
        m[5]  = 1;
        m[10] = 1;
        m[15] = 1;
        return this;
    }

    public Matrix4 set(Matrix4 other) {
        System.arraycopy(other.m, 0, this.m, 0, SIZE);
        return this;
    }
    
    public float get(int index) {
        return m[index];
    }
    
    public float[] getIndices(int... indices) {
        float[] arr = new float[indices.length];
        for (int i = 0; i < indices.length; i++) {
            arr[i] = m[indices[i]];
        }
        return arr;
    }
    
    public float get(int row, int col) {
        return m[row * 4 + col];
    }

    public Matrix4 set(int row, int col, float value) {
        m[row * 4 + col] = value;
        return this;
    }

    public Matrix4 set(float... values) {
        if (values.length != SIZE) throw new IllegalArgumentException("Matrix4 requires 16 values.");
        System.arraycopy(values, 0, m, 0, SIZE);
        return this;
    }
    
    public Matrix4 scale(Vector3 scale) {
        return scale(scale.x, scale.y, scale.z);
    }

    public Matrix4 scale(float x, float y, float z) {
        return multiplyLocal(makeScale(x, y, z));
    }

    public Matrix4 scale(float value) {
        return multiplyLocal(makeScale(value, value, value));
    }
    
    public Matrix4 translate(Vector3 pos) {
        return translate(pos.x, pos.y, pos.z);
    }

    public Matrix4 translate(float x, float y, float z) {
        return multiplyLocal(makeTranslation(x, y, z));
    }
    
    public Matrix4 rotateXYZ(Vector3 rot) {
        return rotateXYZ(rot.x, rot.y, rot.z);
    }

    public Matrix4 rotateXYZ(float x, float y, float z) {
        return rotateX(x).rotateY(y).rotateZ(z);
    }

    public Matrix4 rotateX(float angleRad) {
        return multiplyLocal(makeRotationX(angleRad));
    }

    public Matrix4 rotateY(float angleRad) {
        return multiplyLocal(makeRotationY(angleRad));
    }

    public Matrix4 rotateZ(float angleRad) {
        return multiplyLocal(makeRotationZ(angleRad));
    }

    public Matrix4 rotateXYZ_Degrees(Vector3 rot) {
        return rotateXYZ_Degrees(rot.x, rot.y, rot.z);
    }

    public Matrix4 rotateXYZ_Degrees(float x, float y, float z) {
        return rotateX((float) Math.toRadians(x))
               .rotateY((float) Math.toRadians(y))
               .rotateZ((float) Math.toRadians(z));
    }
    
    public Matrix4 multiply(Matrix4 o) {
        Matrix4 result = new Matrix4(false);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) {
                    sum += this.m[row * 4 + k] * o.m[k * 4 + col];
                }
                result.m[row * 4 + col] = sum;
            }
        }
        return result;
    }

    public Vector3 multiply(Vector3 v) {
        Vector3 o = new Vector3();
        o.x = v.x * m[0]  + v.y * m[4]  + v.z * m[8]  + m[12];
        o.y = v.x * m[1]  + v.y * m[5]  + v.z * m[9]  + m[13];
        o.z = v.x * m[2]  + v.y * m[6]  + v.z * m[10] + m[14];
        o.w = v.x * m[3]  + v.y * m[7]  + v.z * m[11] + m[15];
        return o;
    }
    
    public Triangle multiply(Triangle t) {
        Triangle r = new Triangle(t);
        r.v0.pos = multiply(r.v0.pos);
        r.v1.pos = multiply(r.v1.pos);
        r.v2.pos = multiply(r.v2.pos);
        return r;
    }

    public static Matrix4 transpose(Matrix4 in) {
        Matrix4 out = new Matrix4(false);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                out.set(col, row, in.get(row, col));
            }
        }
        return out;
    }

    public float[] getRaw() {
        return Arrays.copyOf(m, SIZE);
    }
    
    public Matrix4 multiplyLocal(Matrix4 other) {
        float[] result = new float[16];

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) {
                    sum += this.m[row * 4 + k] * other.m[k * 4 + col];
                }
                result[row * 4 + col] = sum;
            }
        }

        System.arraycopy(result, 0, this.m, 0, 16);
        return this;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix4:\n");
        for (int row = 0; row < 4; row++) {
            sb.append("| ");
            for (int col = 0; col < 4; col++) {
                sb.append(String.format("%8.3f ", get(row, col)));
            }
            sb.append("|\n");
        }
        return sb.toString();
    }

    //================
    // Static Utility
    //================

    public static Matrix4 makeTranslation(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m[12] = x;
        m.m[13] = y;
        m.m[14] = z;
        return m;
    }
    
    public static Matrix4 makeScale(float x, float y, float z) {
        Matrix4 m = new Matrix4();
        m.m[0] = x;
        m.m[5] = y;
        m.m[10] = z;
        return m;
    }

    public static Matrix4 makeRotationX(float angleRad) {
        Matrix4 m = new Matrix4();
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        m.m[5] = cos;
        m.m[6] = -sin;
        m.m[9] = sin;
        m.m[10] = cos;
        return m;
    }

    public static Matrix4 makeRotationY(float angleRad) {
        Matrix4 m = new Matrix4();
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        m.m[0] = cos;
        m.m[2] = sin;
        m.m[8] = -sin;
        m.m[10] = cos;
        return m;
    }

    public static Matrix4 makeRotationZ(float angleRad) {
        Matrix4 m = new Matrix4();
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        m.m[0] = cos;
        m.m[1] = -sin;
        m.m[4] = sin;
        m.m[5] = cos;
        return m;
    }

    public static Matrix4 makeProjection(float fovDeg, float aspect, float near, float far) {
        float fovRad = 1.0f / (float) Math.tan(Math.toRadians(fovDeg) / 2.0);
        Matrix4 m = new Matrix4(false);
        m.m[0] = aspect * fovRad;
        m.m[5] = fovRad;
        m.m[10] = far / (far - near);
        m.m[11] = 1.0f;
        m.m[14] = (-far * near) / (far - near);
        return m;
    }

    public static Matrix4 rotate(float angleDeg, Vec3f axis) {
        Matrix4 m = new Matrix4();
        float angleRad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float C = 1.0f - cos;

        float x = axis.x, y = axis.y, z = axis.z;

        m.m[0] = x * x * C + cos;
        m.m[1] = x * y * C - z * sin;
        m.m[2] = x * z * C + y * sin;

        m.m[4] = y * x * C + z * sin;
        m.m[5] = y * y * C + cos;
        m.m[6] = y * z * C - x * sin;

        m.m[8] = z * x * C - y * sin;
        m.m[9] = z * y * C + x * sin;
        m.m[10] = z * z * C + cos;

        return m;
    }
}