package com.timeyaa.flutternordicdfu;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class PathUtils {
    /**
     * Return the path of /storage/emulated/0/Android/data/package/cache.
     *
     * @return the path of /storage/emulated/0/Android/data/package/cache
     */
    public static String getExternalAppCachePath(Context context) {
        if (isExternalStorageDisable()) return "";
        return getAbsolutePath(context.getApplicationContext().getExternalCacheDir());
    }

    private static boolean isExternalStorageDisable() {
        return !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private static String getAbsolutePath(final File file) {
        if (file == null) return "";
        return file.getAbsolutePath();
    }
}
