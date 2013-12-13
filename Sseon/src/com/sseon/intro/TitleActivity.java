package com.sseon.intro;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.sseon.R;
import com.sseon.gui.main.MainActivity;

public class TitleActivity extends Activity implements Runnable {
	private Handler splashDelayedHandler;
	private BluetoothAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_title);
		
		splashDelayedHandler = new Handler();
		splashDelayedHandler.postDelayed(this, 1000);
		
		// 여기서 BT 어댑터 체크를 좀 해줌
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			Toast.makeText(this, "블루투스 장치가 없으므로 종료합니다.", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		adapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		splashDelayedHandler.removeCallbacks(this);
	}

	@Override
	public void run() {
		if (adapter == null) {
			// BT 없으면 그냥 끔
			finish();
		} else {
			startActivity(new Intent(this, MainActivity.class));
			finish();
			
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	}

}
