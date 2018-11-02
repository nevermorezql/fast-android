package com.nevermore.falib.util;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogAdapter;
import com.orhanobut.logger.LogStrategy;
import com.orhanobut.logger.LogcatLogStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * 基于 orhanobut 的开源工具类，简化配置，日志工具类
 */
public class FastLogger {

    /**
     * 每个日志文件的限制
     */
    private static final int MAX_BYTES = 500 * 1024;

    public static class Initializer {
        boolean logOn = true; //默认日志开关
        int priority = 0; //默认日志级别（全部打印）
        String tag = "FastLogger"; //默认日志TAG
        boolean writeToDisk = false; //保存到SD卡
        boolean printLogcat = true; //输出至logCat
        String diskLogPath;

        private Initializer() {
            //默认的日志存储路径
            diskLogPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separatorChar + "fast-android-logger";
        }

        /**
         * 整体的日志开关
         *
         * @param logOn 开关
         * @return
         */
        public Initializer logOn(boolean logOn) {
            this.logOn = logOn;
            return this;
        }

        /**
         * 日志TAG
         *
         * @param tag 默认为FastLogger
         * @return
         */
        public Initializer tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Initializer writeToDisk(boolean writeToDisk) {
            this.writeToDisk = writeToDisk;
            return this;
        }

        public Initializer priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Initializer printLogcat(boolean printLogcat) {
            this.printLogcat = printLogcat;
            return this;
        }

        public Initializer diskLogPath(String diskLogPath) {
            this.diskLogPath = diskLogPath;
            return this;
        }

        /**
         * 获取初始化配置
         *
         * @return
         */
        public static Initializer newInitializer() {
            return new Initializer();
        }

        /**
         * 执行初始化操作
         */
        public void init() {
            if (!logOn) { //不打印日志
                return;
            }
            if (printLogcat) {
                Logger.addLogAdapter(new FastLogAdapter(PrettyFormatStrategy.newBuilder()
                        .methodCount(3)
                        .tag(tag)
                        .showThreadInfo(true)
                        .methodOffset(0)
                        .logStrategy(new LogcatLogStrategy())
                        .build()));
            }

            if (writeToDisk) { //存储到SD卡的日志
                HandlerThread ht = new HandlerThread("AndroidFileLogger." + diskLogPath);
                ht.start();
                Handler handler = new DiskLogStrategy.WriteHandler(ht.getLooper(), diskLogPath, MAX_BYTES);
                Logger.addLogAdapter(new FastLogAdapter(CsvFormatStrategy.newBuilder()
                        .tag(tag)
                        .logStrategy(new DiskLogStrategy(handler))
                        .dateFormat(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss:SSS"))
                        .build()) {
                });
            }
        }


        private class FastLogAdapter implements LogAdapter {
            @NonNull
            private final FormatStrategy formatStrategy;

            public FastLogAdapter(FormatStrategy formatStrategy) {
                this.formatStrategy = checkNotNull(formatStrategy);
            }

            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return priority >= Initializer.this.priority;
            }

            @Override
            public void log(int priority, @Nullable String tag, @NonNull String message) {
                formatStrategy.log(priority, tag, message);
            }
        }

        /**
         * copy from original code
         */
        private static class DiskLogStrategy implements LogStrategy {

            @NonNull
            private final Handler handler;

            public DiskLogStrategy(@NonNull Handler handler) {
                this.handler = checkNotNull(handler);
            }

            @Override
            public void log(int level, @Nullable String tag, @NonNull String message) {
                checkNotNull(message);

                // do nothing on the calling thread, simply pass the tag/msg to the background thread
                handler.sendMessage(handler.obtainMessage(level, message));
            }

            public static class WriteHandler extends Handler {

                @NonNull
                private final String folder;
                private final int maxFileSize;

                WriteHandler(@NonNull Looper looper, @NonNull String folder, int maxFileSize) {
                    super(checkNotNull(looper));
                    this.folder = checkNotNull(folder);
                    this.maxFileSize = maxFileSize;
                }

                @SuppressWarnings("checkstyle:emptyblock")
                @Override
                public void handleMessage(@NonNull Message msg) {
                    String content = (String) msg.obj;

                    FileWriter fileWriter = null;
                    File logFile = getLogFile(folder, "logs");

                    try {
                        fileWriter = new FileWriter(logFile, true);

                        writeLog(fileWriter, content);

                        fileWriter.flush();
                        fileWriter.close();
                    } catch (IOException e) {
                        if (fileWriter != null) {
                            try {
                                fileWriter.flush();
                                fileWriter.close();
                            } catch (IOException e1) { /* fail silently */ }
                        }
                    }
                }

                /**
                 * This is always called on a single background thread.
                 * Implementing classes must ONLY write to the fileWriter and nothing more.
                 * The abstract class takes care of everything else including close the stream and catching IOException
                 *
                 * @param fileWriter an instance of FileWriter already initialised to the correct file
                 */
                private void writeLog(@NonNull FileWriter fileWriter, @NonNull String content) throws IOException {
                    checkNotNull(fileWriter);
                    checkNotNull(content);

                    fileWriter.append(content);
                }

                private File getLogFile(@NonNull String folderName, @NonNull String fileName) {
                    checkNotNull(folderName);
                    checkNotNull(fileName);

                    File folder = new File(folderName);
                    if (!folder.exists()) {
                        //TODO: What if folder is not created, what happens then?
                        folder.mkdirs();
                    }

                    int newFileCount = 0;
                    File newFile;
                    File existingFile = null;

                    newFile = new File(folder, String.format("%s_%s.csv", fileName, newFileCount));
                    while (newFile.exists()) {
                        existingFile = newFile;
                        newFileCount++;
                        newFile = new File(folder, String.format("%s_%s.csv", fileName, newFileCount));
                    }

                    if (existingFile != null) {
                        if (existingFile.length() >= maxFileSize) {
                            return newFile;
                        }
                        return existingFile;
                    }

                    return newFile;
                }
            }
        }

    }

    @NonNull
    static <T> T checkNotNull(@Nullable final T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }
}
