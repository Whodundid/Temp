package controller.globe.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import controller.globe.models.Model;

public class ZipStuff {
    
    static Map<String, byte[]> readZipToMemory(File f) throws IOException {
        Map<String, byte[]> map = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            ZipEntry e;
            byte[] buf = new byte[64 * 1024];
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                var baos = new ByteArrayOutputStream((int) Math.max(1024, e.getSize()));
                int r;
                while ((r = zis.read(buf)) >= 0)
                    baos.write(buf, 0, r);
                map.put(normalizePath(e.getName()), baos.toByteArray());
            }
        }
        return map;
    }
    
    static String findFirstWithExtension(Set<String> keys, String extLower) {
        for (String k : keys)
            if (k.endsWith(extLower)) return k;
        return null;
    }
    
    static String parentDir(String p) {
        int i = p.lastIndexOf('/');
        return (i >= 0) ? p.substring(0, i + 1) : "";
    }
    
    static String resolveZipPath(String baseDir, String rel) {
        String r = rel.replace('\\', '/').replaceAll("/+", "/");
        if (r.startsWith("/")) r = r.substring(1);
        return normalizePath(baseDir + r);
    }
    
    static int ensureTextureFromMap(Model model, Map<String, byte[]> zip, String baseDir, String pathRel) {
        if (pathRel == null) return -1;
        String key = resolveZipPath(baseDir, pathRel);
        byte[] data = zip.get(key);
        if (data == null) return -1;
        try (var bais = new ByteArrayInputStream(data)) {
            var img = ImageIO.read(bais);
            if (img == null) return -1;
            return model.ensureTexture(key, img);
        }
        catch (IOException ignored) {
            return -1;
        }
    }
    
    public static String normalizePath(String p) {
        return p.replace('\\','/').replaceAll("/+","/").toLowerCase(Locale.ROOT);
    }
    
}
