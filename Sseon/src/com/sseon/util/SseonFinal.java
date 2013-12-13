package com.sseon.util;

import android.app.Application;

/**
 * 안드로이드 전역변수 클래스
 *  
 *
 */

public class SseonFinal extends Application {
	
	public static boolean onBTConnect = false;
	public static Thread connThread = null;
	public static boolean isDeviceConnect = false;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}
	
}
