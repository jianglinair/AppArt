package com.lin.jiang.appart.utils;

import android.os.Environment;

public class Constants {
    public static final String CHAPTER_2_PATH = Environment
            .getExternalStorageDirectory().getPath()
            + "/jianglin/chapter2/";

    public static final String CACHE_FILE_PATH = CHAPTER_2_PATH + "usercache";

    public static final int MSG_FROM_CLIENT = 0;
    public static final int MSG_FROM_SERVICE = 1;

}
