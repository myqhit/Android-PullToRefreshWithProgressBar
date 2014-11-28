package com.handmark.pulltorefresh.library;

import android.util.Log;

public class ALog {
	private static final String TAG = "AgileMD";
    public static long getThreadId() {
        return Thread.currentThread().getId();
    }
    public static String getFileName() {
        return Thread.currentThread().getStackTrace()[5].getFileName();
    }
    public static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[5].getLineNumber();
    }
    public static String getMethodName() {
        String method = Thread.currentThread().getStackTrace()[5].getMethodName();
        return method + "()";
    }
	private static String decorateMessage(String message) {
		return "tid:" + getThreadId() + ", " + getFileName() + ":" + getMethodName() + ":" + getLineNumber() + ": " + message;
	}

	public static void e(String message) {
		Log.e(":" + TAG, decorateMessage(message));
	}
	public static void w(String message) {
		Log.w(":" + TAG, decorateMessage(message));
	}
	public static void i(String message) {
		Log.i(":" + TAG, decorateMessage(message));
	}
    public static void d(String message) {
        Log.d(":" + TAG, decorateMessage(message));
    }
    public static void v(String message) {
        Log.v(":" + TAG, decorateMessage(message));
    }

	public static void stack(Exception e) {
		e.printStackTrace();
	}
}