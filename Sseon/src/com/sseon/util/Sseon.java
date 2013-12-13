package com.sseon.util;

import java.util.UUID;

import android.app.Application;

/**
 * 안드로이드 전역변수 클래스
 *  
 *
 */

public class Sseon extends Application {
//	public static boolean onBTConnect = false;
//	public static Thread connThread = null;
//	public static boolean isDeviceConnect = false;

	public static final UUID SerialPortServiceClass_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805f9b34fb");

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}
	
}
