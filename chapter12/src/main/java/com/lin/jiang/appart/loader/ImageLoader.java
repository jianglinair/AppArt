package com.lin.jiang.appart.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.lin.jiang.appart.R;
import com.lin.jiang.appart.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 功能： <br />
 * 1) 图片的同步加载 <br />
 * 2) 图片的异步加载 <br />
 * 3) 图片压缩 <br />
 * 4) 内存缓存 <br />
 * 5) 磁盘缓存 <br />
 * 6) 网络拉取 <br />
 * <p>
 * ============================= <br />
 * 1) Bitmap 的高效加载 <br />
 * 2) LurCache 实现内存缓存 <br />
 * 3) DiskLruCache 实现磁盘缓存 <br />
 * <p>
 * Created by jianglin on 17-9-2.
 */

public class ImageLoader {
    public static final int MESSAGE_POST_RESULT = 1;
    private static final String TAG = "ImageLoader";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1; // 核心线程数
    private static final int MAX_POOL_SIZE = 2 * CPU_COUNT + 1; // 最大线程数
    private static final long KEEP_ALIVE_TIME = 10L; // 线程闲置超时时长10s

    private static final int TAG_KEY_URI = R.id.imageloader_uri;
    private static final long DISK_CACHE_SIZE = 50 * 1024 * 1024; // 磁盘缓存50M
    private static final int IO_BUFFER_SIZE = 8 * 1024; // 8KB
    private static final int DISK_CACHE_INDEX = 0;
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        /**
         * 线程池中创建新线程的方法
         * @param r
         * @return
         */
        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
            KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), THREAD_FACTORY);
    private boolean mIsDiskLruCacheCreated = false;
    /**
     * A Handler created in main thread
     */
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg
         */
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;
            String uri = (String) imageView.getTag(TAG_KEY_URI); // 加载的图片 URI，延迟加载，可能有列表错位问题
            if (uri.equals(result.uri)) {
                imageView.setImageBitmap(result.bitmap);
            } else {
                Log.d(TAG, "handleMessage: ===== 列表错位 =====");
                Log.d(TAG, "handleMessage: 加载的图片 uri: " + uri);
                Log.d(TAG, "handleMessage: 正确的 item uri: " + result.uri);
            }
        }
    };

    private Context mContext;
    private ImageResizer mImageResizer = new ImageResizer();
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    /**
     * 初始化， LruCache 和 DiskLruCache 创建
     *
     * @param context
     */
    private ImageLoader(Context context) {
        mContext = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // KB
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            /**
             * Returns the size of the entry for {@code key} and {@code value} in
             * user-defined units.  The default implementation returns 1 so that size
             * is the number of entries and max size is the maximum number of entries.
             * <p>
             * <p>An entry's size must not change while it is in the cache.
             *
             * @param key
             * @param value
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }

        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * build a new instance of ImageLoader
     *
     * @param context
     * @return a new instance of ImageLoader
     */
    public static ImageLoader build(Context context) {
        return new ImageLoader(context);
    }

    /**
     * 异步加载<br></>
     * load bitmap from cache, disk or network async, then bind it to imageView.<br></>
     * NOTE THAT: should run in UI thread
     *
     * @param uri       http uri
     * @param imageView bitmap's binding object
     */
    public void bindBitmap(final String uri, final ImageView imageView) {
        bindBitmap(uri, imageView, 0, 0);
    }

    /**
     * 异步加载
     *
     * @param uri
     * @param imageView
     * @param reqWidth
     * @param reqHeight
     */
    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight) {
        imageView.setTag(TAG_KEY_URI, uri);
        Bitmap bitmap = loadBitmapFromMemCache(uri); // (1)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight); // (2)
                if (bitmap != null) {
                    LoaderResult result = new LoaderResult(imageView, uri, bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    /**
     * 同步加载， 子线程中调用
     *
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap loadBitmap(String uri, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemCache(uri);// (1)
        if (bitmap != null) {
            Log.d(TAG, "loadBitmap: loadBitmapFromMemCache, url: " + uri);
            return bitmap;
        }

        try {
            bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight); // (2)
            if (bitmap != null) {
                Log.d(TAG, "loadBitmap: loadBitmapFromDiskCache, url: " + uri);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight); // (3)
            Log.d(TAG, "loadBitmap: loadBitmapFromHttp, url: " + uri);
        } catch (IOException e) {
            Log.e(TAG, "loadBitmap: loadBitmapFromDiskCache error, " + e.getMessage());
            e.printStackTrace();
        }

        if (bitmap == null && !mIsDiskLruCacheCreated) {
            Log.w(TAG, "loadBitmap: encounter error, DiskLruCache is not created.");
            bitmap = downloadBitmapFromUrl(uri); // (3)
        }

        return bitmap;
    }

    // ===== LruCache methods start =====

    /**
     * 内存缓存：获取
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 内存缓存：添加
     *
     * @param key
     * @param bitmap
     */
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap loadBitmapFromMemCache(String url) {
        final String key = hashKeyFromUrl(url);
        Bitmap bitmap = getBitmapFromMemCache(key);
        return bitmap;
    }
    // ===== LruCache methods end =====


    // ===== DiskLruCache methods start =====

    /**
     * 磁盘缓存：添加
     *
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        Log.d(TAG, "loadBitmapFromHttp: url: " + url);
        // 验证：必须在子线程中进行下载
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("cannot visit network from UI thread.");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    /**
     * 磁盘缓存：读取
     *
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "loadBitmapFromDiskCache: load bitmap in UI thread, it's not recommended.");
        }
        if (mDiskLruCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fd = fileInputStream.getFD();
            bitmap = mImageResizer.decodeSampledFromFileDescriptor(fd, reqWidth, reqHeight); // 压缩图片
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap); // 加入内存缓存
            }
        }
        Log.d(TAG, "loadBitmapFromDiskCache: " + bitmap);
        return bitmap;
    }
    // ===== DiskLruCache methods end =====


    // ===== Network methods start =====

    /**
     * 下载图片并缓存到磁盘
     *
     * @param urlStr
     * @param outputStream
     * @return
     */
    private boolean downloadUrlToStream(String urlStr, OutputStream outputStream) {
        HttpURLConnection conn = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(conn.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "downloadUrlToStream: download bitmap failed. " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            Util.close(out);
            Util.close(in);
        }
        return false;
    }

    /**
     * 下载图片
     *
     * @param urlStr
     * @return
     */
    private Bitmap downloadBitmapFromUrl(String urlStr) {
        Bitmap bitmap = null;
        HttpURLConnection conn = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(conn.getInputStream(), IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadBitmapFromUrl: download bitmap error!", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            Util.close(in);
        }
        return bitmap;
    }
    // ===== Network methods end =====


    // ===== Util methods start

    /**
     * 获取缓存目录
     *
     * @param context
     * @param uniqueName
     * @return
     */
    private File getDiskCacheDir(Context context, String uniqueName) {
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath(); // data/data/package_name/cache
        }
        return new File(cachePath + File.separator + uniqueName); // /sdcard/Android/data/package_name
    }

    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return stats.getBlockSizeLong() * stats.getAvailableBlocksLong();
    }

    private String hashKeyFromUrl(String url) {
        String key;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            key = bytesToKexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            key = String.valueOf(url.hashCode());
            e.printStackTrace();
        }
        return key;
    }

    private String bytesToKexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0XFF & bytes[i]);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    /**
     * 封装图片加载成功后的图片，图片的 uri 和需要绑定的 ImageView
     */
    private static class LoaderResult {
        public ImageView imageView;
        /**
         * 与 item 绑定的 URI，与列表错位问题有关
         */
        public String uri;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }
}
