package controller.globe.math;

public class Quaternion {
    
    //========
    // Fields
    //========
    
    public float x, y, z, w; // w + xi + yj + zk
    
    //==============
    // Constructors
    //==============
    
    public Quaternion() { this(0, 0, 0, 1); }
    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
    
    //===========
    // Overrides
    //===========
    
    @Override
    public String toString() {
        return String.format("Quaternion(w=%.4f, x=%.4f, y=%.4f, z=%.4f )", w, x, y, z);
    }
    
    //=========
    // Methods
    //=========
    
    public Quaternion mul(Quaternion b) { // this * b
        return new Quaternion(
            w * b.x + x * b.w + y * b.z - z * b.y,
            w * b.y - x * b.z + y * b.w + z * b.x,
            w * b.z + x * b.y - y * b.x + z * b.w,
            w * b.w - x * b.x - y * b.y - z * b.z
        );
    }
    
    public Quaternion normalize() {
        float n = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (n == 0) return this;
        x /= n;
        y /= n;
        z /= n;
        w /= n;
        return this;
    }
    
    // Rotate a vector by this quaternion
    public Vector3 rotate(Vector3 v) {
        // q * (v,0) * conj(q)
        float vx = v.x, vy = v.y, vz = v.z;
        float qx = x, qy = y, qz = z, qw = w;
        // t = 2 * cross(q.xyz, v)
        float tx = 2f * (qy * vz - qz * vy);
        float ty = 2f * (qz * vx - qx * vz);
        float tz = 2f * (qx * vy - qy * vx);
        // v' = v + qw * t + cross(q.xyz, t)
        return new Vector3(
            vx + qw * tx + (qy * tz - qz * ty),
            vy + qw * ty + (qz * tx - qx * tz),
            vz + qw * tz + (qx * ty - qy * tx));
    }
    
    // Get basis vectors (right, up, forward) from quaternion
    public Basis toBasis() {
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        float wx = w * x;
        float wy = w * y;
        float wz = w * z;
        Vector3 right = new Vector3(1 - 2 * (yy + zz), 2 * (xy + wz), 2 * (xz - wy));
        Vector3 up = new Vector3(2 * (xy - wz), 1 - 2 * (xx + zz), 2 * (yz + wx));
        Vector3 forward = new Vector3(2 * (xz + wy), 2 * (yz - wx), 1 - 2 * (xx + yy));
        return new Basis(right.normalize(), up.normalize(), forward.normalize());
    }

    //=======================
    // Static Helper Methods
    //=======================
    
    public static Quaternion identity() {
        return new Quaternion(0, 0, 0, 1);
    }
    
    public static Quaternion fromAxisAngle(Vector3 axis, float angleRad) {
        Vector3 a = new Vector3(axis).normalize();
        float s = (float) Math.sin(angleRad * 0.5f);
        return new Quaternion(a.x * s, a.y * s, a.z * s, (float) Math.cos(angleRad * 0.5f));
    }
    
    /** Build from an orthonormal basis (right, up, forward). */
    public static Quaternion fromBasis(Vector3 r, Vector3 u, Vector3 f) {
        // Rotation matrix (column-vectors = basis)
        float m00=r.x, m01=u.x, m02=f.x;
        float m10=r.y, m11=u.y, m12=f.y;
        float m20=r.z, m21=u.z, m22=f.z;

        float trace = m00 + m11 + m22;
        float x,y,z,w;
        if (trace > 0f) {
            float s = (float)Math.sqrt(trace + 1f) * 2f;
            w = 0.25f * s;
            x = (m21 - m12) / s;
            y = (m02 - m20) / s;
            z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            float s = (float)Math.sqrt(1f + m00 - m11 - m22) * 2f;
            w = (m21 - m12) / s;
            x = 0.25f * s;
            y = (m01 + m10) / s;
            z = (m02 + m20) / s;
        } else if (m11 > m22) {
            float s = (float)Math.sqrt(1f + m11 - m00 - m22) * 2f;
            w = (m02 - m20) / s;
            x = (m01 + m10) / s;
            y = 0.25f * s;
            z = (m12 + m21) / s;
        } else {
            float s = (float)Math.sqrt(1f + m22 - m00 - m11) * 2f;
            w = (m10 - m01) / s;
            x = (m02 + m20) / s;
            y = (m12 + m21) / s;
            z = 0.25f * s;
        }
        return new Quaternion(x,y,z,w).normalize();
    }

    /** Spherical linear interpolation. t in [0,1]. */
    public static Quaternion slerp(Quaternion a, Quaternion b, float t) {
        float dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
        Quaternion bb = (dot < 0f) ? new Quaternion(-b.x, -b.y, -b.z, -b.w) : b; // take shortest path
        dot = Math.abs(dot);

        if (dot > 0.9995f) { // nearly linear
            Quaternion r = new Quaternion(
                a.x + t * (bb.x - a.x),
                a.y + t * (bb.y - a.y),
                a.z + t * (bb.z - a.z),
                a.w + t * (bb.w - a.w)
            );
            return r.normalize();
        }

        double theta0 = Math.acos(dot);
        double theta  = theta0 * t;
        double sin0   = Math.sin(theta0);
        double s0 = Math.cos(theta) - dot * Math.sin(theta) / sin0;
        double s1 = Math.sin(theta) / sin0;

        return new Quaternion(
            (float) (a.x * s0 + bb.x * s1),
            (float) (a.y * s0 + bb.y * s1),
            (float) (a.z * s0 + bb.z * s1),
            (float) (a.w * s0 + bb.w * s1)
        ).normalize();
    }
    
    //================
    // Static Classes
    //================
    
    public static final class Basis {
        public final Vector3 right, up, forward;
        
        public Basis(Vector3 r, Vector3 u, Vector3 f) {
            right = r;
            up = u;
            forward = f;
        }
        
    }
    
}
