package se.lublin.mumla.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamUtils {
    public static byte[] getBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = stream.read(buf)) != -1) {
                outBuf.write(buf, 0, len);
            }
        } finally {
            outBuf.close();
        }
        return outBuf.toByteArray();
    }
}
