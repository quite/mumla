package se.lublin.mumla.util;

import android.graphics.Bitmap;

public class BitmapUtils {
    public static Bitmap resizeKeepingAspect(Bitmap image, int maxWidth, int maxHeight){
        int width = image.getWidth();
        int height = image.getHeight();

        if (width < maxWidth && height < maxHeight) {
            return image;
        }

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
    }
}
