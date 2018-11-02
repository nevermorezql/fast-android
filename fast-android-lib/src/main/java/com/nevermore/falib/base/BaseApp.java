package com.nevermore.falib.base;

import android.os.Environment;
import android.support.multidex.MultiDexApplication;

import com.nevermore.falib.util.FastLogger;

import java.io.File;

/**
 * Created by zhouqinglong on 2018/11/1 15:30.
 * 基础的App
 */

public class BaseApp extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        //日志初始化
        FastLogger
                .Initializer
                .newInitializer()
                .logOn(true)
                .printLogcat(true)
                .diskLogPath(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separatorChar + "fast-android-logger")
                .tag("FAST-ANDROID")
                .init();
    }
}
