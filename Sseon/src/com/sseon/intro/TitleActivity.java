package com.sseon.intro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.sseon.R;
import com.sseon.gui.main.MainActivity;

public class TitleActivity extends Activity implements Runnable {
	private Handler splashDelayedHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_title);
		
		splashDelayedHandler = new Handler();
		splashDelayedHandler.postDelayed(this, 1000);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		splashDelayedHandler.removeCallbacks(this);
	}

	@Override
	public void run() {
		startActivity(new Intent(this, MainActivity.class));
		finish();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

}
