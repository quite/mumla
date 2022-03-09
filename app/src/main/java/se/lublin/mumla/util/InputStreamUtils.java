package se.lublin.mumla.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class InputStreamUtils {
    public static byte[] getBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tempBuffer = new byte[1024];

        int length;
        while((length = stream.read(tempBuffer)) != -1){
            buffer.write(tempBuffer, 0, length);
        }

        return buffer.toByteArray();
    }
}
