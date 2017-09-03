package com.lin.jiang.appart.loader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * 图片压缩功能的实现
 * <p>
 * Created by jianglin on 17-9-2.
 */

public class ImageResizer {
    private static final String TAG = "ImageResizer";

    public ImageResizer() {
    }

    /**
     * 从内存高效加载 Bitmap
     * <p>
     * 1) 将 BitmapFactory.Options 的 inJustDecodeBounds 参数设为 true 并加载图片; <br />
     * 2) 从 BitmapFactory.Options 中取出图片的原始宽高信息，他们对应于 outWidth 和 outHeight 参数; <br />
     * 3) 根据采样率的规则并结合目标 View 的所需大小计算出采样率 inSampleSize; <br />
     * 4) 将 BitmapFactory.Options 的 inJustDecodeBounds 参数设为 false，然后重新加载图片 <br />
     *
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap width inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 从磁盘文件高效加载 Bitmap
     * <p>
     * 之所以使用 BitmapFactory.decodeFileDescriptor() 而不是 BitmapFactory.decodeResource() 是因为，
     * FileInputStream 是一种有序的文件流，而两次使用 decodeStream 影响了文件流的位置属性，会导致第二次
     * decodeStream 时得到的是 null
     *
     * @param fd
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampledFromFileDescriptor(FileDescriptor fd, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap width inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        if (reqWidth == 0 || reqHeight == 0)
            return 1;

        // Raw width and height of image
        final int width = options.outWidth;
        final int height = options.outHeight;
        Log.d(TAG, "calculateInSampleSize: image's original size: " + width + "*" + height);

        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;

            while ((halfWidth / inSampleSize >= reqWidth) && (halfHeight / inSampleSize >= reqHeight)) {
                inSampleSize *= 2;
            }
        }
        Log.d(TAG, "calculateInSampleSize: inSampleSize=" + inSampleSize);
        return inSampleSize;
    }

}
