package com.nevermore.falib.util;

import android.app.Application;
import android.support.annotation.StringRes;
import android.widget.Toast;

/**
 * Created by zhouqinglong on 2018/4/26/026
 * <p>
 * 提示消息工具类，根据的需求还可以加入snackBar
 */

public class TipManager {

    private static Application context;

    private static Toast toast;


    public static void init(Application context) {
        TipManager.context = context;
    }

    /**
     * 提示消息
     *
     * @param message 消息内容
     */
    public static void toastShort(String message) {
        if (null != toast) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * 提示消息
     *
     * @param message 消息内容
     */
    public static void toastLong(String message) {
        if (null != toast) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * 提示消息
     *
     * @param msgId 消息ID
     */
    public static void toastShort(@StringRes int msgId) {
        toastShort(context.getResources().getString(msgId));
    }

    /**
     * 提示消息
     *
     * @param msgId 消息ID
     */
    public static void toastLong(@StringRes int msgId) {
        toastLong(context.getResources().getString(msgId));
    }
}
